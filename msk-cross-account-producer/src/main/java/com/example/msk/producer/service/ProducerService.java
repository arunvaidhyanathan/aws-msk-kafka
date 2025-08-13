package com.example.msk.producer.service;

import com.example.msk.producer.dto.ProduceMessageRequest;
import com.example.msk.producer.dto.ProduceMessageResponse;
import com.example.msk.producer.dto.ProducerMetrics;
import com.example.msk.producer.model.TestMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProducerService {
    
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    @Value("${aws.msk.topic-name}")
    private String topicName;
    
    @Value("${producer.rate-limit-per-second}")
    private Integer rateLimitPerSecond;
    
    // Metrics tracking
    private final AtomicLong totalMessagesProduced = new AtomicLong(0);
    private final AtomicLong totalSuccessfulMessages = new AtomicLong(0);
    private final AtomicLong totalFailedMessages = new AtomicLong(0);
    private final AtomicLong totalBatches = new AtomicLong(0);
    private volatile Instant lastProductionTime;
    private volatile Instant serviceStartTime = Instant.now();
    
    public ProduceMessageResponse produceMessages(ProduceMessageRequest request) {
        String batchId = UUID.randomUUID().toString();
        List<String> messageIds = new ArrayList<>();
        Instant startTime = Instant.now();
        
        int successCount = 0;
        int failureCount = 0;
        StringBuilder errorDetails = new StringBuilder();
        
        log.info("Starting batch production - Batch ID: {}, Size: {}", batchId, request.getBatchSize());
        
        try {
            for (int i = 0; i < request.getBatchSize(); i++) {
                String messageId = UUID.randomUUID().toString();
                messageIds.add(messageId);
                
                TestMessage message = TestMessage.builder()
                    .messageId(messageId)
                    .timestamp(Instant.now())
                    .sourceAccount(request.getSourceAccount())
                    .targetAccount(request.getTargetAccount())
                    .payload(request.getPayload())
                    .messageType(request.getMessageType())
                    .batchId(batchId)
                    .sequenceNumber(i + 1)
                    .build();
                
                try {
                    CompletableFuture<SendResult<String, Object>> future = 
                        kafkaTemplate.send(topicName, messageId, message);
                    
                    future.whenComplete((result, ex) -> {
                        if (ex == null) {
                            totalSuccessfulMessages.incrementAndGet();
                            log.debug("Message sent successfully - ID: {}, Partition: {}, Offset: {}", 
                                messageId, result.getRecordMetadata().partition(), 
                                result.getRecordMetadata().offset());
                        } else {
                            totalFailedMessages.incrementAndGet();
                            log.error("Failed to send message - ID: {}", messageId, ex);
                        }
                    });
                    
                    successCount++;
                    totalMessagesProduced.incrementAndGet();
                    
                    // Apply rate limiting
                    if (i > 0 && i % rateLimitPerSecond == 0) {
                        Thread.sleep(1000);
                    }
                    
                } catch (Exception e) {
                    failureCount++;
                    totalFailedMessages.incrementAndGet();
                    errorDetails.append("Message ").append(messageId).append(": ")
                        .append(e.getMessage()).append("; ");
                    log.error("Error sending message {}", messageId, e);
                }
            }
            
            totalBatches.incrementAndGet();
            lastProductionTime = Instant.now();
            
        } catch (Exception e) {
            log.error("Batch production failed", e);
            errorDetails.append("Batch error: ").append(e.getMessage());
        }
        
        Instant endTime = Instant.now();
        long durationMs = endTime.toEpochMilli() - startTime.toEpochMilli();
        
        ProduceMessageResponse response = ProduceMessageResponse.builder()
            .batchId(batchId)
            .messagesSent(request.getBatchSize())
            .successCount(successCount)
            .failureCount(failureCount)
            .startTime(startTime)
            .endTime(endTime)
            .durationMs(durationMs)
            .messageIds(messageIds)
            .errorDetails(errorDetails.length() > 0 ? errorDetails.toString() : null)
            .build();
        
        log.info("Batch production completed - Batch ID: {}, Success: {}, Failed: {}, Duration: {}ms",
            batchId, successCount, failureCount, durationMs);
        
        return response;
    }
    
    public ProducerMetrics getMetrics() {
        long totalMessages = totalMessagesProduced.get();
        long successMessages = totalSuccessfulMessages.get();
        long failedMessages = totalFailedMessages.get();
        long batches = totalBatches.get();
        
        double successRate = totalMessages > 0 ? 
            (double) successMessages / totalMessages * 100 : 0;
        
        long runtimeSeconds = Instant.now().getEpochSecond() - serviceStartTime.getEpochSecond();
        double avgMessagesPerSecond = runtimeSeconds > 0 ? 
            (double) totalMessages / runtimeSeconds : 0;
        
        double avgBatchSize = batches > 0 ? 
            (double) totalMessages / batches : 0;
        
        return ProducerMetrics.builder()
            .totalMessagesProduced(totalMessages)
            .totalSuccessfulMessages(successMessages)
            .totalFailedMessages(failedMessages)
            .avgMessagesPerSecond(avgMessagesPerSecond)
            .lastProductionTime(lastProductionTime != null ? lastProductionTime.toString() : "Never")
            .connectionStatus("Connected") // This could be enhanced with actual connection checking
            .totalBatches(batches)
            .avgBatchSize(avgBatchSize)
            .successRate(successRate)
            .build();
    }
}