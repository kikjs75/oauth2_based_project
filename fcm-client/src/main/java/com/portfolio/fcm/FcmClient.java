package com.portfolio.fcm;

import com.portfolio.oauth2.awt.core.AssertionTokenClient;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * FCM HTTP v1 API client with Micrometer metrics.
 */
public class FcmClient {

    private static final Logger log = LoggerFactory.getLogger(FcmClient.class);

    private final FcmConfig config;
    private final AssertionTokenClient tokenClient;
    private final RestClient restClient;
    private final Timer sendTimer;

    public FcmClient(FcmConfig config,
                     AssertionTokenClient tokenClient,
                     RestClient restClient,
                     MeterRegistry meterRegistry) {
        this.config = config;
        this.tokenClient = tokenClient;
        this.restClient = restClient;
        this.sendTimer = Timer.builder("fcm.send.duration")
                .description("FCM message send latency")
                .register(meterRegistry);
    }

    public void send(FcmMessage message) {
        sendTimer.record(() -> doSend(message));
    }

    private void doSend(FcmMessage message) {
        String accessToken = tokenClient.getAccessToken();
        String endpoint = config.resolvedSendEndpoint();

        log.debug("Sending FCM message to token: {}...", message.token().substring(0, Math.min(10, message.token().length())));

        Map<String, Object> body = message.toRequestBody();

        restClient.post()
                .uri(endpoint)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toBodilessEntity();

        log.info("FCM message sent successfully");
    }
}
