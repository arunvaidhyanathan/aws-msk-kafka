Product Requirements Document (PRD)
Spring Boot Applications for Cross-Account MSK Connectivity Testing
Updated Architecture Overview
Account A (MSK Provider)          Account B (Application Account)
├── MSK Cluster (IAM Auth)        ├── EKS Cluster
├── Multi-VPC Connectivity        │   ├── Spring Boot Producer
├── Cluster Policy                │   └── Spring Boot Consumer
└── Cross-Account IAM Role        ├── Aurora Database
                                  └── MSK VPC Connection
Application Requirements
Application 1: Spring Boot Kafka Producer
Purpose: Test message production to cross-account MSK cluster using IAM role assumption
Core Functionality:

Message Generation: Create sample test messages with metadata
Cross-Account Authentication: Assume IAM role in Account A for MSK access
Batch Processing: Support configurable batch sizes for testing throughput
Health Monitoring: Expose health endpoints for EKS readiness/liveness probes

Technical Specifications:
yamlApplication Name: msk-cross-account-producer
Spring Boot Version: 3.2.x
Java Version: 17+
Packaging: JAR with embedded Tomcat
Key Features:

Message Schema:
json{
  "messageId": "uuid",
  "timestamp": "ISO-8601",
  "sourceAccount": "account-b",
  "targetAccount": "account-a",
  "payload": "test-data",
  "messageType": "connectivity-test",
  "batchId": "uuid",
  "sequenceNumber": "integer"
}

Configuration Management:

Externalized configuration via ConfigMaps/Secrets
Environment-specific profiles (dev, test, prod)
MSK cluster endpoints and topic configuration


IAM Role Assumption:

Use AWS STS to assume role in Account A
Temporary credential management and refresh
Error handling for assumption failures


Production Patterns:

Configurable message rate (messages/second)
Burst testing capability
Message deduplication support
Producer acknowledgment strategies



API Endpoints:

POST /api/v1/produce - Trigger message production
GET /api/v1/health - Application health check
GET /api/v1/metrics - Production metrics
POST /api/v1/test/batch - Batch production test

Application 2: Spring Boot Kafka Consumer
Purpose: Consume messages from cross-account MSK and persist to Aurora database
Core Functionality:

Message Consumption: Subscribe to test topics with proper error handling
Database Persistence: Store consumed messages in Aurora with metadata
Cross-Account Authentication: Same role assumption pattern as producer
Message Processing: Validate message integrity and track consumption metrics

Technical Specifications:
yamlApplication Name: msk-cross-account-consumer
Spring Boot Version: 3.2.x
Java Version: 17
Database: Aurora PostgreSQL
ORM: Spring Data JPA
Key Features:

Consumer Configuration:

Configurable consumer groups for parallel processing
Offset management strategies
Dead letter queue handling for failed messages
Backpressure management


Database Schema:
sqlCREATE TABLE consumed_messages (
  id SERIAL PRIMARY KEY,
  message_id VARCHAR(255) UNIQUE NOT NULL,
  original_timestamp TIMESTAMP NOT NULL,
  consumed_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  source_account VARCHAR(50),
  message_type VARCHAR(100),
  payload JSONB,
  batch_id VARCHAR(255),
  sequence_number INTEGER,
  processing_duration_ms INTEGER
);

Message Processing Pipeline:

Message validation and schema verification
Duplicate detection and handling
Transaction management for database operations
Metrics collection for processing performance


Error Handling:

Retry mechanisms with exponential backoff
Dead letter topic for unprocessable messages
Alert integration for processing failures



API Endpoints:

GET /api/v1/consumer/status - Consumer status and lag information
GET /api/v1/consumer/metrics - Consumption metrics
POST /api/v1/consumer/reset - Reset consumer offsets (admin only)
GET /api/v1/messages - Query consumed messages with filters

Shared Technical Requirements
Dependencies (Both Applications)
xml<!-- AWS MSK IAM Authentication -->
<dependency>
    <groupId>software.amazon.msk</groupId>
    <artifactId>aws-msk-iam-auth</artifactId>
    <version>1.1.4</version>
</dependency>

<!-- Spring Kafka -->
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka</artifactId>
</dependency>

<!-- AWS SDK for STS (Role Assumption) -->
<dependency>
    <groupId>software.amazon.awssdk</groupId>
    <artifactId>sts</artifactId>
    <version>2.21.x</version>
</dependency>

<!-- Observability -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-cloudwatch</artifactId>
</dependency>
Configuration Template
yaml# application.yml (shared configuration)
spring:
  kafka:
    bootstrap-servers: ${MSK_BOOTSTRAP_SERVERS}
    properties:
      security.protocol: SASL_SSL
      sasl.mechanism: AWS_MSK_IAM
      sasl.jaas.config: software.amazon.msk.auth.iam.IAMLoginModule required;
      sasl.client.callback.handler.class: software.amazon.msk.auth.iam.IAMClientCallbackHandler

aws:
  region: ${AWS_REGION:us-east-1}
  msk:
    cluster-arn: ${MSK_CLUSTER_ARN}
    role-arn: ${CROSS_ACCOUNT_ROLE_ARN} # Role in Account A
    topic-name: ${KAFKA_TOPIC:connectivity-test}

management:
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus
  metrics:
    export:
      cloudwatch:
        enabled: true
EKS Deployment Specifications
ServiceAccount Configuration
yamlapiVersion: v1
kind: ServiceAccount
metadata:
  name: msk-test-service-account
  annotations:
    eks.amazonaws.com/role-arn: arn:aws:iam::ACCOUNT-B:role/EKSMSKTestRole
Deployment Strategy

Resource Limits: CPU: 500m, Memory: 1Gi per application
Replicas: 1 producer, 2 consumers (for redundancy)
Health Checks: Kubernetes readiness/liveness probes
ConfigMaps: Environment-specific configuration
Secrets: Sensitive credentials and connection strings

Testing Framework Requirements
Test Scenarios

Connectivity Test: Verify cross-account MSK access
Authentication Test: Validate IAM role assumption
Throughput Test: Measure message production/consumption rates
Reliability Test: Test error handling and recovery
Latency Test: Measure end-to-end message delivery time

Test Data Patterns

Volume Testing: 1K, 10K, 100K messages
Rate Testing: 10, 100, 1000 messages/second
Size Testing: Small (1KB), Medium (10KB), Large (100KB) messages
Failure Testing: Network interruptions, authentication failures

Monitoring & Observability
Application Metrics
Producer Metrics:

Messages produced per second
Production success/failure rates
Role assumption success rate
Average message size

Consumer Metrics:

Messages consumed per second
Consumer lag by partition
Database insertion success rate
Processing latency (message to database)

Health Indicators

MSK cluster connectivity status
Aurora database connectivity
IAM role assumption health
Kafka topic availability

Success Criteria
Functional Requirements

✅ Cross-Account Access: Applications can authenticate to Account A's MSK
✅ Message Flow: Producer → MSK → Consumer → Aurora pipeline works
✅ Error Recovery: Applications recover from transient failures
✅ Data Integrity: All produced messages are consumed and stored

Performance Requirements

Latency: < 200ms end-to-end (produce → consume → store)
Throughput: Support 1000+ messages/second sustained
Reliability: 99.9% message delivery success rate
Recovery: < 30 seconds to recover from failures

Deployment Pipeline
CI/CD Requirements

Build Stage: Maven/Gradle build with unit tests
Security Scan: Container image vulnerability scanning
Integration Tests: Test against MSK cluster
Deployment: Rolling deployment to EKS
Smoke Tests: Post-deployment connectivity verification

Environment Promotion

Development: Single replica, relaxed monitoring
Testing: Multi-replica, full monitoring, load testing
Production: High availability, alerting, auto-scaling
