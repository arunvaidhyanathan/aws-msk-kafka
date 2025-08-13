package com.example.msk.consumer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Consumed message details")
public class ConsumedMessageDTO {
    
    @Schema(description = "Database ID")
    private Long id;
    
    @Schema(description = "Unique message ID")
    private String messageId;
    
    @Schema(description = "Original message timestamp")
    private Instant originalTimestamp;
    
    @Schema(description = "Timestamp when consumed")
    private Instant consumedTimestamp;
    
    @Schema(description = "Source AWS account")
    private String sourceAccount;
    
    @Schema(description = "Target AWS account")
    private String targetAccount;
    
    @Schema(description = "Message type")
    private String messageType;
    
    @Schema(description = "Message payload")
    private Map<String, Object> payload;
    
    @Schema(description = "Batch ID")
    private String batchId;
    
    @Schema(description = "Sequence number in batch")
    private Integer sequenceNumber;
    
    @Schema(description = "Processing duration in milliseconds")
    private Long processingDurationMs;
    
    @Schema(description = "Kafka partition")
    private Integer kafkaPartition;
    
    @Schema(description = "Kafka offset")
    private Long kafkaOffset;
    
    @Schema(description = "Processing status")
    private String processingStatus;
    
    @Schema(description = "Error message if failed")
    private String errorMessage;
}