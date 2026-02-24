package com.portfolio.fcm;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "fcm")
public record FcmConfig(
        String projectId,
        String sendEndpoint
) {
    public String resolvedSendEndpoint() {
        if (sendEndpoint != null) return sendEndpoint;
        return "https://fcm.googleapis.com/v1/projects/" + projectId + "/messages:send";
    }
}
