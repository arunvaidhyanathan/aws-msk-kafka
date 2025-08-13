package com.example.msk.consumer.entity;

import com.vladmihalcea.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;

import java.time.Instant;
import java.util.Map;

@Entity
@Table(name = "consumed_messages", indexes = {
    @Index(name = "idx_message_id", columnList = "message_id"),
    @Index(name = "idx_batch_id", columnList = "batch_id"),
    @Index(name = "idx_consumed_timestamp", columnList = "consumed_timestamp")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsumedMessage {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "message_id", unique = true, nullable = false)
    private String messageId;
    
    @Column(name = "original_timestamp", nullable = false)
    private Instant originalTimestamp;
    
    @Column(name = "consumed_timestamp", nullable = false)
    private Instant consumedTimestamp;
    
    @Column(name = "source_account", length = 50)
    private String sourceAccount;
    
    @Column(name = "target_account", length = 50)
    private String targetAccount;
    
    @Column(name = "message_type", length = 100)
    private String messageType;
    
    @Type(JsonType.class)
    @Column(name = "payload", columnDefinition = "jsonb")
    private Map<String, Object> payload;
    
    @Column(name = "batch_id")
    private String batchId;
    
    @Column(name = "sequence_number")
    private Integer sequenceNumber;
    
    @Column(name = "processing_duration_ms")
    private Long processingDurationMs;
    
    @Column(name = "kafka_partition")
    private Integer kafkaPartition;
    
    @Column(name = "kafka_offset")
    private Long kafkaOffset;
    
    @Column(name = "consumer_group")
    private String consumerGroup;
    
    @Column(name = "processing_status")
    @Enumerated(EnumType.STRING)
    private ProcessingStatus processingStatus;
    
    @Column(name = "error_message", length = 1000)
    private String errorMessage;
    
    @Column(name = "retry_count")
    private Integer retryCount;
    
    public enum ProcessingStatus {
        SUCCESS, FAILED, DLQ
    }
}