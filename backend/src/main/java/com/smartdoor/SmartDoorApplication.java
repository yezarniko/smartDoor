package com.smartdoor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class SmartDoorApplication {
    public static void main(String[] args) {
        SpringApplication.run(SmartDoorApplication.class, args);
    }
}

