package com.example.msk.consumer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Consumer status and metrics response")
public class ConsumerStatusResponse {
    
    @Schema(description = "Consumer group ID")
    private String consumerGroup;
    
    @Schema(description = "Current consumer status")
    private String status;
    
    @Schema(description = "Total messages consumed")
    private Long totalMessagesConsumed;
    
    @Schema(description = "Total successful messages")
    private Long totalSuccessfulMessages;
    
    @Schema(description = "Total failed messages")
    private Long totalFailedMessages;
    
    @Schema(description = "Success rate percentage")
    private Double successRate;
    
    @Schema(description = "Last consumption timestamp")
    private String lastConsumptionTime;
    
    @Schema(description = "Consumer lag by partition")
    private Map<Integer, Long> partitionLag;
    
    @Schema(description = "Average processing time in milliseconds")
    private Double avgProcessingTimeMs;
    
    @Schema(description = "Messages consumed in last hour")
    private Long messagesLastHour;
    
    @Schema(description = "Database connection status")
    private String databaseStatus;
}