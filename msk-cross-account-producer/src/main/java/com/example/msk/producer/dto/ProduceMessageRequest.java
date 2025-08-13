package com.example.msk.producer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Request to produce messages to MSK")
public class ProduceMessageRequest {
    
    @Schema(description = "Payload content for the message", example = "Test message content")
    @NotBlank(message = "Payload cannot be blank")
    private String payload;
    
    @Schema(description = "Type of message to produce", example = "connectivity-test")
    private String messageType = "connectivity-test";
    
    @Schema(description = "Number of messages to produce in this batch", example = "10")
    @Min(value = 1, message = "Batch size must be at least 1")
    @Max(value = 1000, message = "Batch size cannot exceed 1000")
    private Integer batchSize = 1;
    
    @Schema(description = "Source AWS account ID", example = "123456789012")
    private String sourceAccount;
    
    @Schema(description = "Target AWS account ID", example = "987654321098")
    private String targetAccount;
}