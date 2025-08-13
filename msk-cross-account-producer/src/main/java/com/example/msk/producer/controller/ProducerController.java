package com.example.msk.producer.controller;

import com.example.msk.producer.dto.ProduceMessageRequest;
import com.example.msk.producer.dto.ProduceMessageResponse;
import com.example.msk.producer.dto.ProducerMetrics;
import com.example.msk.producer.service.ProducerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "MSK Producer", description = "Endpoints for producing messages to AWS MSK")
public class ProducerController {
    
    private final ProducerService producerService;
    
    @PostMapping("/produce")
    @Operation(summary = "Produce messages to MSK", 
        description = "Produces a batch of messages to the configured MSK topic")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Messages produced successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ProduceMessageResponse> produceMessages(
            @Valid @RequestBody ProduceMessageRequest request) {
        log.info("Received produce request - Batch size: {}, Type: {}", 
            request.getBatchSize(), request.getMessageType());
        
        try {
            ProduceMessageResponse response = producerService.produceMessages(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error producing messages", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ProduceMessageResponse.builder()
                    .errorDetails("Failed to produce messages: " + e.getMessage())
                    .build());
        }
    }
    
    @PostMapping("/test/batch")
    @Operation(summary = "Batch production test", 
        description = "Performs a batch production test with predefined configurations")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Batch test completed"),
        @ApiResponse(responseCode = "500", description = "Test failed")
    })
    public ResponseEntity<ProduceMessageResponse> batchTest(
            @RequestParam(defaultValue = "100") Integer messageCount,
            @RequestParam(defaultValue = "test-batch") String payloadPrefix) {
        
        ProduceMessageRequest request = new ProduceMessageRequest();
        request.setBatchSize(messageCount);
        request.setPayload(payloadPrefix + " - Batch test message");
        request.setMessageType("batch-test");
        request.setSourceAccount("account-b");
        request.setTargetAccount("account-a");
        
        try {
            ProduceMessageResponse response = producerService.produceMessages(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Batch test failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ProduceMessageResponse.builder()
                    .errorDetails("Batch test failed: " + e.getMessage())
                    .build());
        }
    }
    
    @GetMapping("/metrics")
    @Operation(summary = "Get producer metrics", 
        description = "Returns current producer metrics and statistics")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Metrics retrieved successfully")
    })
    public ResponseEntity<ProducerMetrics> getMetrics() {
        return ResponseEntity.ok(producerService.getMetrics());
    }
    
    @GetMapping("/health")
    @Operation(summary = "Health check", 
        description = "Check if the producer service is healthy")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Service is healthy")
    })
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Producer service is healthy");
    }
}