package com.portfolio.fcm;

import java.util.Map;

/**
 * Represents an FCM HTTP v1 message payload.
 */
public record FcmMessage(
        String token,
        String title,
        String body,
        Map<String, String> data
) {
    public Map<String, Object> toRequestBody() {
        Map<String, Object> notification = Map.of("title", title, "body", body);
        Map<String, Object> message = new java.util.LinkedHashMap<>();
        message.put("token", token);
        message.put("notification", notification);
        if (data != null && !data.isEmpty()) {
            message.put("data", data);
        }
        return Map.of("message", message);
    }
}
