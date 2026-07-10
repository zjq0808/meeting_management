package com.example.meeting;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.example.meeting.mapper")
public class MeetingManagementApplication {
    public static void main(String[] args) {
        SpringApplication.run(MeetingManagementApplication.class, args);
    }
}
