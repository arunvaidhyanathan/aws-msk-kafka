package com.example.msk.producer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response after producing messages to MSK")
public class ProduceMessageResponse {
    
    @Schema(description = "Unique batch ID for this production run")
    private String batchId;
    
    @Schema(description = "Total number of messages sent")
    private Integer messagesSent;
    
    @Schema(description = "Number of successful messages")
    private Integer successCount;
    
    @Schema(description = "Number of failed messages")
    private Integer failureCount;
    
    @Schema(description = "Timestamp when production started")
    private Instant startTime;
    
    @Schema(description = "Timestamp when production completed")
    private Instant endTime;
    
    @Schema(description = "Total duration in milliseconds")
    private Long durationMs;
    
    @Schema(description = "List of message IDs produced")
    private List<String> messageIds;
    
    @Schema(description = "Error details if any failures occurred")
    private String errorDetails;
}