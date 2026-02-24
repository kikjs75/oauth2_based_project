package com.portfolio.oauth2.awt.starter;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.util.List;

@ConfigurationProperties(prefix = "oauth2.awt")
public record Oauth2AwtProperties(
        Google google,
        Microsoft microsoft
) {
    public record Google(
            String serviceAccountKeyPath,
            @DefaultValue("https://oauth2.googleapis.com/token") String tokenEndpoint,
            List<String> scopes
    ) {}

    public record Microsoft(
            String clientId,
            String tenantId,
            String privateKeyPemPath,
            String keyId,
            @DefaultValue("https://login.microsoftonline.com/{tenant}/oauth2/v2.0/token") String tokenEndpoint,
            List<String> scopes
    ) {}
}
