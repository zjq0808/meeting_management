package com.example.meeting.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class LoggingNotificationSender implements NotificationSender {
    private static final Logger log = LoggerFactory.getLogger(LoggingNotificationSender.class);

    @Override
    public boolean send(Map<String, Object> notification) {
        log.info("mock notification sent: {}", notification);
        return true;
    }
}
