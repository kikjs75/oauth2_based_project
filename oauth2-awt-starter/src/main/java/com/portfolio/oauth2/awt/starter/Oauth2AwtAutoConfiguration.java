package com.portfolio.oauth2.awt.starter;

import com.portfolio.oauth2.awt.core.AssertionConfig;
import com.portfolio.oauth2.awt.core.AssertionTokenClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.nio.file.Files;
import java.nio.file.Paths;

@AutoConfiguration
@EnableConfigurationProperties(Oauth2AwtProperties.class)
public class Oauth2AwtAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(Oauth2AwtAutoConfiguration.class);

    @Bean(name = "googleAssertionTokenClient")
    @ConditionalOnExpression("'${oauth2.awt.google.service-account-key-path:}' != ''")
    public AssertionTokenClient googleAssertionTokenClient(Oauth2AwtProperties props) throws Exception {
        Oauth2AwtProperties.Google google = props.google();
        String pemContent = readServiceAccountKeyAsPem(google.serviceAccountKeyPath());

        AssertionConfig config = AssertionConfig.builder()
                .provider(AssertionConfig.Provider.GOOGLE)
                .clientId(extractClientEmail(google.serviceAccountKeyPath()))
                .tokenEndpoint(google.tokenEndpoint())
                .scopes(google.scopes())
                .privateKeyPem(pemContent)
                .clockSkewSeconds(60)
                .tokenExpirySeconds(3600)
                .maxRetries(3)
                .timeoutMs(10_000)
                .build();

        log.info("Configured Google AssertionTokenClient for scopes: {}", google.scopes());
        return new AssertionTokenClient(config);
    }

    @Bean(name = "microsoftAssertionTokenClient")
    @ConditionalOnExpression("'${oauth2.awt.microsoft.client-id:}' != ''")
    public AssertionTokenClient microsoftAssertionTokenClient(Oauth2AwtProperties props) throws Exception {
        Oauth2AwtProperties.Microsoft ms = props.microsoft();
        String pemContent = new String(Files.readAllBytes(Paths.get(ms.privateKeyPemPath())));
        String endpoint = ms.tokenEndpoint().replace("{tenant}", ms.tenantId());

        AssertionConfig config = AssertionConfig.builder()
                .provider(AssertionConfig.Provider.MICROSOFT)
                .clientId(ms.clientId())
                .tokenEndpoint(endpoint)
                .scopes(ms.scopes())
                .privateKeyPem(pemContent)
                .keyId(ms.keyId())
                .clockSkewSeconds(60)
                .tokenExpirySeconds(3600)
                .maxRetries(3)
                .timeoutMs(10_000)
                .build();

        log.info("Configured Microsoft AssertionTokenClient for clientId: {}", ms.clientId());
        return new AssertionTokenClient(config);
    }

    /**
     * Reads the private_key field from Google service account JSON and extracts PEM.
     */
    private String readServiceAccountKeyAsPem(String keyPath) throws Exception {
        String json = new String(Files.readAllBytes(Paths.get(keyPath)));
        // Extract private_key value from JSON (simple string extraction — no JSON parser dependency)
        int start = json.indexOf("\"private_key\"");
        if (start == -1) throw new IllegalArgumentException("No private_key field in service account JSON");
        int colon = json.indexOf(':', start);
        int quote1 = json.indexOf('"', colon + 1);
        int quote2 = json.indexOf("\",", quote1 + 1);
        if (quote2 == -1) quote2 = json.lastIndexOf('"');
        return json.substring(quote1 + 1, quote2).replace("\\n", "\n");
    }

    /**
     * Extracts client_email from Google service account JSON.
     */
    private String extractClientEmail(String keyPath) throws Exception {
        String json = new String(Files.readAllBytes(Paths.get(keyPath)));
        int start = json.indexOf("\"client_email\"");
        if (start == -1) throw new IllegalArgumentException("No client_email field in service account JSON");
        int colon = json.indexOf(':', start);
        int quote1 = json.indexOf('"', colon + 1);
        int quote2 = json.indexOf('"', quote1 + 1);
        return json.substring(quote1 + 1, quote2);
    }
}
