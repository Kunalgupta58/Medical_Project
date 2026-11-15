package com.eldercare.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ElderCare360Application {
    public static void main(String[] args) {
        SpringApplication.run(ElderCare360Application.class, args);
    }
}
