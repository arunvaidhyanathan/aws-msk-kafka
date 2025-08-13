-- Database initialization script for MSK Consumer
CREATE DATABASE IF NOT EXISTS msk_consumer;

-- Create the consumed_messages table
CREATE TABLE IF NOT EXISTS consumed_messages (
  id SERIAL PRIMARY KEY,
  message_id VARCHAR(255) UNIQUE NOT NULL,
  original_timestamp TIMESTAMP NOT NULL,
  consumed_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  source_account VARCHAR(50),
  target_account VARCHAR(50),
  message_type VARCHAR(100),
  payload JSONB,
  batch_id VARCHAR(255),
  sequence_number INTEGER,
  processing_duration_ms INTEGER,
  kafka_partition INTEGER,
  kafka_offset BIGINT,
  consumer_group VARCHAR(255),
  processing_status VARCHAR(20) CHECK (processing_status IN ('SUCCESS', 'FAILED', 'DLQ')),
  error_message VARCHAR(1000),
  retry_count INTEGER DEFAULT 0
);

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_message_id ON consumed_messages(message_id);
CREATE INDEX IF NOT EXISTS idx_batch_id ON consumed_messages(batch_id);
CREATE INDEX IF NOT EXISTS idx_consumed_timestamp ON consumed_messages(consumed_timestamp);
CREATE INDEX IF NOT EXISTS idx_message_type ON consumed_messages(message_type);
CREATE INDEX IF NOT EXISTS idx_processing_status ON consumed_messages(processing_status);
CREATE INDEX IF NOT EXISTS idx_kafka_partition_offset ON consumed_messages(kafka_partition, kafka_offset);

-- Insert sample data for testing (optional)
INSERT INTO consumed_messages (
  message_id, 
  original_timestamp, 
  source_account, 
  target_account, 
  message_type, 
  payload, 
  batch_id, 
  sequence_number,
  processing_duration_ms,
  kafka_partition,
  kafka_offset,
  consumer_group,
  processing_status
) VALUES (
  'sample-message-001',
  NOW() - INTERVAL '1 hour',
  'account-b',
  'account-a',
  'sample-test',
  '{"messageId": "sample-message-001", "payload": "Sample test message"}',
  'sample-batch-001',
  1,
  150,
  0,
  0,
  'msk-cross-account-consumer-group',
  'SUCCESS'
) ON CONFLICT (message_id) DO NOTHING;