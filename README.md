# AWS MSK Cross-Account Connectivity Testing

This project provides Spring Boot applications for testing cross-account connectivity to AWS MSK (Managed Streaming for Apache Kafka).

## Architecture Overview

- **Producer Application**: Sends messages to MSK cluster in Account A
- **Consumer Application**: Consumes messages from MSK and persists to Aurora PostgreSQL
- **Cross-Account Authentication**: Uses IAM role assumption for secure access

## Project Structure

```
AWS_MSK/
├── msk-cross-account-producer/     # Producer Spring Boot application
├── msk-cross-account-consumer/     # Consumer Spring Boot application
├── postman-collection/             # Postman testing collection
├── docker-compose.yml              # Local development setup
└── README.md                       # This file
```

## Prerequisites

- Java 17+
- Maven 3.6+
- PostgreSQL (for consumer)
- AWS CLI configured
- MSK cluster with IAM authentication enabled

## Quick Start

### 1. Environment Setup

Set the following environment variables:

```bash
# MSK Configuration
export MSK_BOOTSTRAP_SERVERS="your-msk-bootstrap-servers"
export MSK_CLUSTER_ARN="arn:aws:kafka:region:account:cluster/cluster-name"
export CROSS_ACCOUNT_ROLE_ARN="arn:aws:iam::account:role/CrossAccountMSKRole"
export KAFKA_TOPIC="connectivity-test"

# Database Configuration (for consumer)
export DATABASE_URL="jdbc:postgresql://localhost:5432/msk_consumer"
export DATABASE_USERNAME="postgres"
export DATABASE_PASSWORD="password"

# AWS Configuration
export AWS_REGION="us-east-1"
```

### 2. Build Applications

```bash
# Build Producer
cd msk-cross-account-producer
mvn clean package -DskipTests

# Build Consumer
cd ../msk-cross-account-consumer
mvn clean package -DskipTests
```

### 3. Run Applications

```bash
# Start Producer (Port 8080)
cd msk-cross-account-producer
java -jar target/msk-cross-account-producer-1.0.0.jar

# Start Consumer (Port 8081)
cd ../msk-cross-account-consumer
java -jar target/msk-cross-account-consumer-1.0.0.jar
```

### 4. Using Docker Compose (Recommended for local development)

```bash
docker-compose up -d
```

## API Documentation

### Producer API (Port 8080)

- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **Health Check**: `GET /api/v1/health`
- **Produce Messages**: `POST /api/v1/produce`
- **Batch Test**: `POST /api/v1/test/batch`
- **Metrics**: `GET /api/v1/metrics`

### Consumer API (Port 8081)

- **Swagger UI**: http://localhost:8081/swagger-ui.html
- **Health Check**: `GET /api/v1/consumer/health`
- **Status**: `GET /api/v1/consumer/status`
- **Query Messages**: `GET /api/v1/consumer/messages`
- **Metrics**: `GET /api/v1/consumer/metrics`

## Testing with Postman

1. Import the collection: `postman-collection/MSK-Cross-Account-Testing.postman_collection.json`
2. Import the environment: `postman-collection/MSK-Environment.postman_environment.json`
3. Run the "End-to-End Tests" folder to validate the complete flow

## Message Flow

1. **Producer** creates test messages with metadata
2. Messages are sent to MSK topic using IAM authentication
3. **Consumer** receives messages from MSK
4. Messages are validated and persisted to PostgreSQL
5. Processing metrics are tracked and exposed via APIs

## Message Schema

```json
{
  "messageId": "uuid",
  "timestamp": "ISO-8601",
  "sourceAccount": "account-b",
  "targetAccount": "account-a", 
  "payload": "test-data",
  "messageType": "connectivity-test",
  "batchId": "uuid",
  "sequenceNumber": 1
}
```

## Key Features

### Producer
- Configurable batch sizes (1-1000 messages)
- Rate limiting support
- Cross-account IAM role assumption
- Comprehensive metrics tracking
- OpenAPI/Swagger documentation

### Consumer
- Automatic message deduplication
- Database persistence with JSONB payload
- Processing status tracking (SUCCESS/FAILED/DLQ)
- Consumer lag monitoring
- Query API with filtering and pagination

## Configuration

### Producer Configuration (application.yml)
```yaml
aws:
  msk:
    cluster-arn: ${MSK_CLUSTER_ARN}
    role-arn: ${CROSS_ACCOUNT_ROLE_ARN}
    topic-name: ${KAFKA_TOPIC:connectivity-test}
    
producer:
  default-batch-size: 10
  max-batch-size: 1000
  rate-limit-per-second: 100
```

### Consumer Configuration (application.yml)
```yaml
spring:
  datasource:
    url: ${DATABASE_URL}
    username: ${DATABASE_USERNAME}
    password: ${DATABASE_PASSWORD}
    
consumer:
  retry:
    max-attempts: 3
    backoff-ms: 1000
```

## Monitoring

Both applications expose metrics via:
- Spring Boot Actuator endpoints
- Custom metrics APIs
- CloudWatch integration (configurable)

## Security

- IAM role-based authentication for MSK access
- Cross-account role assumption
- Secure credential handling
- No hardcoded secrets

## Troubleshooting

### Common Issues

1. **Authentication Failures**
   - Verify IAM role permissions
   - Check role assumption configuration
   - Validate MSK cluster policy

2. **Connection Issues**
   - Confirm MSK bootstrap servers
   - Check VPC connectivity
   - Verify security group rules

3. **Database Issues**
   - Ensure PostgreSQL is running
   - Verify database credentials
   - Check network connectivity

### Logs

Enable debug logging:
```yaml
logging:
  level:
    com.example.msk: DEBUG
    org.apache.kafka: INFO
```

## Performance Testing

Use the Postman collection's batch tests:
- Single message test
- 10 message batch
- 100 message batch
- 1000 message load test

Monitor metrics through the `/metrics` endpoints during testing.

## Deployment

### Docker
```bash
docker build -t msk-producer ./msk-cross-account-producer
docker build -t msk-consumer ./msk-cross-account-consumer
```

### Kubernetes
Deploy using the provided Kubernetes manifests (see deployment/ directory).

## Contributing

1. Follow Spring Boot best practices
2. Maintain comprehensive test coverage
3. Update documentation for any API changes
4. Follow security guidelines for credential handling