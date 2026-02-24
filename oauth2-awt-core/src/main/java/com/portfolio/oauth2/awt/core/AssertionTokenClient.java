package com.portfolio.oauth2.awt.core;

import org.springframework.web.client.RestClient;

/**
 * Public API: obtains OAuth2 access tokens via JWT Bearer assertion.
 */
public class AssertionTokenClient {

    private final CachedTokenProvider cachedTokenProvider;

    public AssertionTokenClient(AssertionConfig config) {
        RestClient restClient = RestClient.builder()
                .defaultHeader("Accept", "application/json")
                .build();
        JwtAssertionBuilder assertionBuilder = new JwtAssertionBuilder(config);
        TokenEndpointClient endpointClient = new TokenEndpointClient(restClient);
        this.cachedTokenProvider = new CachedTokenProvider(config, assertionBuilder, endpointClient);
    }

    /** Constructor for testing/custom RestClient injection. */
    public AssertionTokenClient(AssertionConfig config, RestClient restClient) {
        JwtAssertionBuilder assertionBuilder = new JwtAssertionBuilder(config);
        TokenEndpointClient endpointClient = new TokenEndpointClient(restClient);
        this.cachedTokenProvider = new CachedTokenProvider(config, assertionBuilder, endpointClient);
    }

    public String getAccessToken() {
        return cachedTokenProvider.getAccessToken();
    }
}
