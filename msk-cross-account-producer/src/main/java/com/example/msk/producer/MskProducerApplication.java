package com.example.msk.producer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

@SpringBootApplication
@EnableKafka
public class MskProducerApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(MskProducerApplication.class, args);
    }
}