package com.portfolio.app.config;

import com.portfolio.fcm.FcmClient;
import com.portfolio.fcm.FcmConfig;
import com.portfolio.oauth2.awt.core.AssertionTokenClient;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(FcmConfig.class)
public class FcmClientConfig {

    @Bean
    @ConditionalOnBean(name = "googleAssertionTokenClient")
    public FcmClient fcmClient(FcmConfig fcmConfig,
                               AssertionTokenClient googleAssertionTokenClient,
                               MeterRegistry meterRegistry) {
        RestClient restClient = RestClient.builder()
                .defaultHeader("Accept", "application/json")
                .build();
        return new FcmClient(fcmConfig, googleAssertionTokenClient, restClient, meterRegistry);
    }
}
