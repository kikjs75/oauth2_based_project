package com.portfolio.oauth2.awt.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.Map;

/**
 * Posts a JWT assertion to a token endpoint and parses the access token.
 */
public class TokenEndpointClient {

    private static final Logger log = LoggerFactory.getLogger(TokenEndpointClient.class);
    private static final String GRANT_TYPE = "urn:ietf:params:oauth:grant-type:jwt-bearer";

    private final RestClient restClient;

    public TokenEndpointClient(RestClient restClient) {
        this.restClient = restClient;
    }

    @SuppressWarnings("unchecked")
    public TokenResponse exchangeAssertion(String tokenEndpoint, String assertion) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", GRANT_TYPE);
        form.add("assertion", assertion);

        log.debug("Exchanging JWT assertion at {}", tokenEndpoint);

        Map<String, Object> body = restClient.post()
                .uri(tokenEndpoint)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(Map.class);

        if (body == null || !body.containsKey("access_token")) {
            throw new IllegalStateException("No access_token in response from " + tokenEndpoint);
        }

        String accessToken = (String) body.get("access_token");
        Number expiresIn = (Number) body.getOrDefault("expires_in", 3600);
        Instant expiresAt = Instant.now().plusSeconds(expiresIn.longValue());

        return new TokenResponse(accessToken, expiresAt);
    }
}
