package com.example.meeting.service;

import java.util.Map;

public interface NotificationSender {
    boolean send(Map<String, Object> notification);
}
