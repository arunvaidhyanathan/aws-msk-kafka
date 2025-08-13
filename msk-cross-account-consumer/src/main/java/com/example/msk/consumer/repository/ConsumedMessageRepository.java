package com.example.msk.consumer.repository;

import com.example.msk.consumer.entity.ConsumedMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface ConsumedMessageRepository extends JpaRepository<ConsumedMessage, Long> {
    
    Optional<ConsumedMessage> findByMessageId(String messageId);
    
    boolean existsByMessageId(String messageId);
    
    List<ConsumedMessage> findByBatchId(String batchId);
    
    Page<ConsumedMessage> findByConsumedTimestampBetween(
        Instant start, Instant end, Pageable pageable);
    
    Page<ConsumedMessage> findByProcessingStatus(
        ConsumedMessage.ProcessingStatus status, Pageable pageable);
    
    @Query("SELECT COUNT(c) FROM ConsumedMessage c WHERE c.consumedTimestamp >= :start")
    Long countMessagesConsumedSince(@Param("start") Instant start);
    
    @Query("SELECT AVG(c.processingDurationMs) FROM ConsumedMessage c WHERE c.consumedTimestamp >= :start")
    Double getAverageProcessingTimeSince(@Param("start") Instant start);
    
    @Query("SELECT c.messageType, COUNT(c) FROM ConsumedMessage c GROUP BY c.messageType")
    List<Object[]> getMessageTypeDistribution();
    
    @Query("SELECT c.processingStatus, COUNT(c) FROM ConsumedMessage c GROUP BY c.processingStatus")
    List<Object[]> getProcessingStatusDistribution();
    
    List<ConsumedMessage> findTop100ByOrderByConsumedTimestampDesc();
}