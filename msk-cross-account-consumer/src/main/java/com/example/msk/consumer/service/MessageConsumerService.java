package com.example.msk.consumer.service;

import com.example.msk.consumer.entity.ConsumedMessage;
import com.example.msk.consumer.model.TestMessage;
import com.example.msk.consumer.repository.ConsumedMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessageConsumerService {
    
    private final ConsumedMessageRepository repository;
    
    // Metrics tracking
    private final AtomicLong totalMessagesConsumed = new AtomicLong(0);
    private final AtomicLong totalSuccessfulMessages = new AtomicLong(0);
    private final AtomicLong totalFailedMessages = new AtomicLong(0);
    private volatile Instant lastConsumptionTime;
    
    @KafkaListener(topics = "${aws.msk.topic-name}", groupId = "${spring.kafka.consumer.group-id}")
    @Transactional
    public void consumeMessage(
            @Payload TestMessage message,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            @Header(KafkaHeaders.GROUP_ID) String groupId,
            @Header(KafkaHeaders.RECEIVED_TIMESTAMP) long timestamp,
            Acknowledgment acknowledgment) {
        
        Instant startTime = Instant.now();
        log.info("Consuming message - ID: {}, Partition: {}, Offset: {}", 
            message.getMessageId(), partition, offset);
        
        try {
            // Check for duplicate message
            if (repository.existsByMessageId(message.getMessageId())) {
                log.warn("Duplicate message detected - ID: {}", message.getMessageId());
                acknowledgment.acknowledge();
                return;
            }
            
            // Process and save message
            ConsumedMessage consumedMessage = ConsumedMessage.builder()
                .messageId(message.getMessageId())
                .originalTimestamp(message.getTimestamp())
                .consumedTimestamp(Instant.now())
                .sourceAccount(message.getSourceAccount())
                .targetAccount(message.getTargetAccount())
                .messageType(message.getMessageType())
                .payload(convertToMap(message))
                .batchId(message.getBatchId())
                .sequenceNumber(message.getSequenceNumber())
                .kafkaPartition(partition)
                .kafkaOffset(offset)
                .consumerGroup(groupId)
                .processingStatus(ConsumedMessage.ProcessingStatus.SUCCESS)
                .retryCount(0)
                .build();
            
            // Calculate processing duration
            long processingDuration = Instant.now().toEpochMilli() - startTime.toEpochMilli();
            consumedMessage.setProcessingDurationMs(processingDuration);
            
            repository.save(consumedMessage);
            
            totalMessagesConsumed.incrementAndGet();
            totalSuccessfulMessages.incrementAndGet();
            lastConsumptionTime = Instant.now();
            
            acknowledgment.acknowledge();
            
            log.debug("Message processed successfully - ID: {}, Duration: {}ms", 
                message.getMessageId(), processingDuration);
            
        } catch (Exception e) {
            log.error("Error processing message - ID: {}", message.getMessageId(), e);
            totalFailedMessages.incrementAndGet();
            
            // Save failed message with error details
            try {
                ConsumedMessage failedMessage = ConsumedMessage.builder()
                    .messageId(message.getMessageId())
                    .originalTimestamp(message.getTimestamp())
                    .consumedTimestamp(Instant.now())
                    .sourceAccount(message.getSourceAccount())
                    .targetAccount(message.getTargetAccount())
                    .messageType(message.getMessageType())
                    .payload(convertToMap(message))
                    .batchId(message.getBatchId())
                    .sequenceNumber(message.getSequenceNumber())
                    .kafkaPartition(partition)
                    .kafkaOffset(offset)
                    .consumerGroup(groupId)
                    .processingStatus(ConsumedMessage.ProcessingStatus.FAILED)
                    .errorMessage(e.getMessage())
                    .retryCount(0)
                    .processingDurationMs(Instant.now().toEpochMilli() - startTime.toEpochMilli())
                    .build();
                
                repository.save(failedMessage);
                acknowledgment.acknowledge(); // Acknowledge even on failure to avoid reprocessing
                
            } catch (Exception saveException) {
                log.error("Failed to save error record", saveException);
                // Don't acknowledge - let it retry
            }
        }
    }
    
    private Map<String, Object> convertToMap(TestMessage message) {
        Map<String, Object> map = new HashMap<>();
        map.put("messageId", message.getMessageId());
        map.put("timestamp", message.getTimestamp() != null ? message.getTimestamp().toString() : null);
        map.put("sourceAccount", message.getSourceAccount());
        map.put("targetAccount", message.getTargetAccount());
        map.put("payload", message.getPayload());
        map.put("messageType", message.getMessageType());
        map.put("batchId", message.getBatchId());
        map.put("sequenceNumber", message.getSequenceNumber());
        return map;
    }
    
    public Map<String, Object> getConsumerMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("totalMessagesConsumed", totalMessagesConsumed.get());
        metrics.put("totalSuccessfulMessages", totalSuccessfulMessages.get());
        metrics.put("totalFailedMessages", totalFailedMessages.get());
        metrics.put("lastConsumptionTime", lastConsumptionTime != null ? lastConsumptionTime.toString() : "Never");
        
        double successRate = totalMessagesConsumed.get() > 0 ? 
            (double) totalSuccessfulMessages.get() / totalMessagesConsumed.get() * 100 : 0;
        metrics.put("successRate", successRate);
        
        return metrics;
    }
}