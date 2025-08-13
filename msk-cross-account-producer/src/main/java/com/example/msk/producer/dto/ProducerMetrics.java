package com.example.msk.producer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Producer metrics and statistics")
public class ProducerMetrics {
    
    @Schema(description = "Total messages produced since startup")
    private Long totalMessagesProduced;
    
    @Schema(description = "Total successful messages")
    private Long totalSuccessfulMessages;
    
    @Schema(description = "Total failed messages")
    private Long totalFailedMessages;
    
    @Schema(description = "Average messages per second")
    private Double avgMessagesPerSecond;
    
    @Schema(description = "Last production timestamp")
    private String lastProductionTime;
    
    @Schema(description = "Current Kafka connection status")
    private String connectionStatus;
    
    @Schema(description = "Total batches produced")
    private Long totalBatches;
    
    @Schema(description = "Average batch size")
    private Double avgBatchSize;
    
    @Schema(description = "Success rate percentage")
    private Double successRate;
}