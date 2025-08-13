package com.example.msk.consumer.controller;

import com.example.msk.consumer.dto.ConsumedMessageDTO;
import com.example.msk.consumer.dto.ConsumerStatusResponse;
import com.example.msk.consumer.dto.MessageQueryRequest;
import com.example.msk.consumer.entity.ConsumedMessage;
import com.example.msk.consumer.repository.ConsumedMessageRepository;
import com.example.msk.consumer.service.MessageConsumerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/consumer")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "MSK Consumer", description = "Endpoints for monitoring and managing message consumption")
public class ConsumerController {
    
    private final MessageConsumerService consumerService;
    private final ConsumedMessageRepository repository;
    
    @Value("${spring.kafka.consumer.group-id}")
    private String consumerGroup;
    
    @GetMapping("/status")
    @Operation(summary = "Get consumer status", 
        description = "Returns current consumer status and metrics")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Status retrieved successfully")
    })
    public ResponseEntity<ConsumerStatusResponse> getConsumerStatus() {
        Map<String, Object> metrics = consumerService.getConsumerMetrics();
        
        // Get additional metrics from database
        Instant oneHourAgo = Instant.now().minus(1, ChronoUnit.HOURS);
        Long messagesLastHour = repository.countMessagesConsumedSince(oneHourAgo);
        Double avgProcessingTime = repository.getAverageProcessingTimeSince(oneHourAgo);
        
        ConsumerStatusResponse response = ConsumerStatusResponse.builder()
            .consumerGroup(consumerGroup)
            .status("ACTIVE")
            .totalMessagesConsumed((Long) metrics.get("totalMessagesConsumed"))
            .totalSuccessfulMessages((Long) metrics.get("totalSuccessfulMessages"))
            .totalFailedMessages((Long) metrics.get("totalFailedMessages"))
            .successRate((Double) metrics.get("successRate"))
            .lastConsumptionTime((String) metrics.get("lastConsumptionTime"))
            .avgProcessingTimeMs(avgProcessingTime != null ? avgProcessingTime : 0.0)
            .messagesLastHour(messagesLastHour)
            .databaseStatus("CONNECTED")
            .build();
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/metrics")
    @Operation(summary = "Get detailed metrics", 
        description = "Returns detailed consumption metrics and statistics")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Metrics retrieved successfully")
    })
    public ResponseEntity<Map<String, Object>> getDetailedMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        
        // Get basic metrics
        metrics.putAll(consumerService.getConsumerMetrics());
        
        // Get message type distribution
        List<Object[]> typeDistribution = repository.getMessageTypeDistribution();
        Map<String, Long> typeMap = typeDistribution.stream()
            .collect(Collectors.toMap(
                arr -> (String) arr[0],
                arr -> (Long) arr[1]
            ));
        metrics.put("messageTypeDistribution", typeMap);
        
        // Get processing status distribution
        List<Object[]> statusDistribution = repository.getProcessingStatusDistribution();
        Map<String, Long> statusMap = statusDistribution.stream()
            .collect(Collectors.toMap(
                arr -> arr[0].toString(),
                arr -> (Long) arr[1]
            ));
        metrics.put("processingStatusDistribution", statusMap);
        
        // Get recent messages count
        metrics.put("messagesLast5Minutes", 
            repository.countMessagesConsumedSince(Instant.now().minus(5, ChronoUnit.MINUTES)));
        metrics.put("messagesLast1Hour", 
            repository.countMessagesConsumedSince(Instant.now().minus(1, ChronoUnit.HOURS)));
        metrics.put("messagesLast24Hours", 
            repository.countMessagesConsumedSince(Instant.now().minus(24, ChronoUnit.HOURS)));
        
        return ResponseEntity.ok(metrics);
    }
    
    @PostMapping("/reset")
    @Operation(summary = "Reset consumer offsets", 
        description = "Resets consumer group offsets (admin only)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Offsets reset successfully"),
        @ApiResponse(responseCode = "403", description = "Unauthorized")
    })
    public ResponseEntity<String> resetOffsets(
            @RequestHeader(value = "X-Admin-Token", required = false) String adminToken) {
        
        // Simple token check - in production, use proper authentication
        if (!"admin-secret-token".equals(adminToken)) {
            return ResponseEntity.status(403).body("Unauthorized");
        }
        
        log.warn("Consumer offset reset requested");
        // Implementation would involve Kafka AdminClient to reset offsets
        return ResponseEntity.ok("Consumer offsets reset functionality not implemented in demo");
    }
    
    @GetMapping("/messages")
    @Operation(summary = "Query consumed messages", 
        description = "Query consumed messages with filtering and pagination")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Messages retrieved successfully")
    })
    public ResponseEntity<Page<ConsumedMessageDTO>> queryMessages(
            @ModelAttribute MessageQueryRequest request) {
        
        Sort sort = Sort.by(
            request.getSortDirection().equalsIgnoreCase("ASC") ? 
                Sort.Direction.ASC : Sort.Direction.DESC,
            request.getSortBy()
        );
        
        PageRequest pageRequest = PageRequest.of(request.getPage(), request.getSize(), sort);
        
        Page<ConsumedMessage> messages;
        
        if (request.getStartTime() != null && request.getEndTime() != null) {
            messages = repository.findByConsumedTimestampBetween(
                request.getStartTime(), request.getEndTime(), pageRequest);
        } else if (request.getProcessingStatus() != null) {
            ConsumedMessage.ProcessingStatus status = 
                ConsumedMessage.ProcessingStatus.valueOf(request.getProcessingStatus());
            messages = repository.findByProcessingStatus(status, pageRequest);
        } else {
            messages = repository.findAll(pageRequest);
        }
        
        Page<ConsumedMessageDTO> dtoPage = messages.map(this::toDTO);
        return ResponseEntity.ok(dtoPage);
    }
    
    @GetMapping("/messages/recent")
    @Operation(summary = "Get recent messages", 
        description = "Returns the most recent 100 messages")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Messages retrieved successfully")
    })
    public ResponseEntity<List<ConsumedMessageDTO>> getRecentMessages() {
        List<ConsumedMessage> recentMessages = repository.findTop100ByOrderByConsumedTimestampDesc();
        List<ConsumedMessageDTO> dtos = recentMessages.stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }
    
    @GetMapping("/messages/{messageId}")
    @Operation(summary = "Get message by ID", 
        description = "Returns a specific consumed message by its ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Message found"),
        @ApiResponse(responseCode = "404", description = "Message not found")
    })
    public ResponseEntity<ConsumedMessageDTO> getMessageById(@PathVariable String messageId) {
        return repository.findByMessageId(messageId)
            .map(this::toDTO)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/health")
    @Operation(summary = "Health check", 
        description = "Check if the consumer service is healthy")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Service is healthy")
    })
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Consumer service is healthy");
    }
    
    private ConsumedMessageDTO toDTO(ConsumedMessage entity) {
        return ConsumedMessageDTO.builder()
            .id(entity.getId())
            .messageId(entity.getMessageId())
            .originalTimestamp(entity.getOriginalTimestamp())
            .consumedTimestamp(entity.getConsumedTimestamp())
            .sourceAccount(entity.getSourceAccount())
            .targetAccount(entity.getTargetAccount())
            .messageType(entity.getMessageType())
            .payload(entity.getPayload())
            .batchId(entity.getBatchId())
            .sequenceNumber(entity.getSequenceNumber())
            .processingDurationMs(entity.getProcessingDurationMs())
            .kafkaPartition(entity.getKafkaPartition())
            .kafkaOffset(entity.getKafkaOffset())
            .processingStatus(entity.getProcessingStatus().toString())
            .errorMessage(entity.getErrorMessage())
            .build();
    }
}