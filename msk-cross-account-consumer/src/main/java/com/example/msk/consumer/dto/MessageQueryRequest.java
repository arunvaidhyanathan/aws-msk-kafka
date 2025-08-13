package com.example.msk.consumer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.Instant;

@Data
@Schema(description = "Request to query consumed messages")
public class MessageQueryRequest {
    
    @Schema(description = "Start timestamp for filtering")
    private Instant startTime;
    
    @Schema(description = "End timestamp for filtering")
    private Instant endTime;
    
    @Schema(description = "Filter by message type")
    private String messageType;
    
    @Schema(description = "Filter by batch ID")
    private String batchId;
    
    @Schema(description = "Filter by processing status (SUCCESS, FAILED, DLQ)")
    private String processingStatus;
    
    @Schema(description = "Page number (0-based)")
    private Integer page = 0;
    
    @Schema(description = "Page size")
    private Integer size = 20;
    
    @Schema(description = "Sort field (default: consumedTimestamp)")
    private String sortBy = "consumedTimestamp";
    
    @Schema(description = "Sort direction (ASC or DESC)")
    private String sortDirection = "DESC";
}