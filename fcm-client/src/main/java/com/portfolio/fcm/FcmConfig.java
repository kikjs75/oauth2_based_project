package com.portfolio.fcm;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "fcm")
public record FcmConfig(
        String projectId,
        String sendEndpoint
) {
    public FcmConfig {
        if (projectId == null && sendEndpoint == null) {
            throw new IllegalArgumentException("projectId must not be null when sendEndpoint is not configured");
        }
    }

    public String resolvedSendEndpoint() {
        if (sendEndpoint != null) return sendEndpoint;
        return "https://fcm.googleapis.com/v1/projects/" + projectId + "/messages:send";
    }
}
