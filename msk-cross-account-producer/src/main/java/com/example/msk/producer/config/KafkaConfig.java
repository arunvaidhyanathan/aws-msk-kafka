package com.example.msk.producer.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.model.AssumeRoleRequest;
import software.amazon.awssdk.services.sts.model.AssumeRoleResponse;
import software.amazon.awssdk.services.sts.model.Credentials;

import java.util.HashMap;
import java.util.Map;

@Configuration
@Slf4j
public class KafkaConfig {
    
    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;
    
    @Value("${aws.region}")
    private String awsRegion;
    
    @Value("${aws.msk.role-arn}")
    private String crossAccountRoleArn;
    
    @Value("${aws.msk.role-session-name}")
    private String roleSessionName;
    
    @Value("${aws.msk.role-duration-seconds}")
    private Integer roleDurationSeconds;
    
    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        configProps.put(ProducerConfig.ACKS_CONFIG, "all");
        configProps.put(ProducerConfig.RETRIES_CONFIG, 3);
        configProps.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);
        configProps.put(ProducerConfig.LINGER_MS_CONFIG, 10);
        configProps.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432);
        
        // MSK IAM Authentication
        configProps.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_SSL");
        configProps.put(SaslConfigs.SASL_MECHANISM, "AWS_MSK_IAM");
        configProps.put(SaslConfigs.SASL_JAAS_CONFIG, 
            "software.amazon.msk.auth.iam.IAMLoginModule required;");
        configProps.put(SaslConfigs.SASL_CLIENT_CALLBACK_HANDLER_CLASS, 
            "software.amazon.msk.auth.iam.IAMClientCallbackHandler");
        
        // Configure AWS credentials for cross-account access
        configureAwsCredentials();
        
        log.info("Kafka producer configuration initialized for MSK cluster");
        return new DefaultKafkaProducerFactory<>(configProps);
    }
    
    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
    
    private void configureAwsCredentials() {
        try {
            // Set up STS client to assume cross-account role
            StsClient stsClient = StsClient.builder()
                .region(Region.of(awsRegion))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
            
            AssumeRoleRequest assumeRoleRequest = AssumeRoleRequest.builder()
                .roleArn(crossAccountRoleArn)
                .roleSessionName(roleSessionName)
                .durationSeconds(roleDurationSeconds)
                .build();
            
            AssumeRoleResponse assumeRoleResponse = stsClient.assumeRole(assumeRoleRequest);
            Credentials credentials = assumeRoleResponse.credentials();
            
            // Set the assumed role credentials as system properties for MSK IAM auth
            System.setProperty("aws.accessKeyId", credentials.accessKeyId());
            System.setProperty("aws.secretAccessKey", credentials.secretAccessKey());
            System.setProperty("aws.sessionToken", credentials.sessionToken());
            
            log.info("Successfully assumed cross-account role: {}", crossAccountRoleArn);
            
        } catch (Exception e) {
            log.error("Failed to assume cross-account role", e);
            throw new RuntimeException("Failed to configure cross-account access", e);
        }
    }
}