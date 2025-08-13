package com.example.msk.consumer.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestMessage {
    private String messageId;
    private Instant timestamp;
    private String sourceAccount;
    private String targetAccount;
    private String payload;
    private String messageType;
    private String batchId;
    private Integer sequenceNumber;
}