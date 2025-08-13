package com.example.msk.consumer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

@SpringBootApplication
@EnableKafka
public class MskConsumerApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(MskConsumerApplication.class, args);
    }
}