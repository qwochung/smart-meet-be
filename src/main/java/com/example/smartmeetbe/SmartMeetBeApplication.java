package com.example.smartmeetbe;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class SmartMeetBeApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartMeetBeApplication.class, args);
    }

}
