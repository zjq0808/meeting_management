package com.example.meeting.util;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Map;

public final class Maps {
    private Maps() {
    }

    public static Long longValue(Map<String, Object> map, String... keys) {
        Object value = value(map, keys);
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return Long.valueOf(String.valueOf(value));
    }

    public static Integer intValue(Map<String, Object> map, String... keys) {
        Object value = value(map, keys);
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return Integer.valueOf(String.valueOf(value));
    }

    public static String stringValue(Map<String, Object> map, String... keys) {
        Object value = value(map, keys);
        return value == null ? null : String.valueOf(value);
    }

    public static LocalDateTime dateTimeValue(Map<String, Object> map, String... keys) {
        Object value = value(map, keys);
        if (value == null) {
            return null;
        }
        if (value instanceof LocalDateTime) {
            return (LocalDateTime) value;
        }
        if (value instanceof Timestamp) {
            return ((Timestamp) value).toLocalDateTime();
        }
        return LocalDateTime.parse(String.valueOf(value).replace(" ", "T"));
    }

    private static Object value(Map<String, Object> map, String... keys) {
        if (map == null) {
            return null;
        }
        for (String key : keys) {
            if (map.containsKey(key)) {
                return map.get(key);
            }
            String lower = key.toLowerCase();
            if (map.containsKey(lower)) {
                return map.get(lower);
            }
            String upper = key.toUpperCase();
            if (map.containsKey(upper)) {
                return map.get(upper);
            }
            String snake = key.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
            if (map.containsKey(snake)) {
                return map.get(snake);
            }
            String upperSnake = snake.toUpperCase();
            if (map.containsKey(upperSnake)) {
                return map.get(upperSnake);
            }
        }
        return null;
    }
}
