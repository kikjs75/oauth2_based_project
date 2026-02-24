package com.portfolio.oauth2.awt.core;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Wraps token acquisition with Caffeine caching and single-flight refresh guard.
 */
public class CachedTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(CachedTokenProvider.class);
    private static final String CACHE_KEY = "token";

    private final AssertionConfig config;
    private final JwtAssertionBuilder assertionBuilder;
    private final TokenEndpointClient endpointClient;
    private final Cache<String, TokenResponse> cache;
    private final ReentrantLock refreshLock = new ReentrantLock();

    public CachedTokenProvider(AssertionConfig config,
                                JwtAssertionBuilder assertionBuilder,
                                TokenEndpointClient endpointClient) {
        this.config = config;
        this.assertionBuilder = assertionBuilder;
        this.endpointClient = endpointClient;
        this.cache = Caffeine.newBuilder()
                .expireAfterWrite(config.tokenExpirySeconds() - config.clockSkewSeconds(), TimeUnit.SECONDS)
                .maximumSize(1)
                .build();
    }

    public String getAccessToken() {
        TokenResponse cached = cache.getIfPresent(CACHE_KEY);
        if (cached != null && !cached.isExpiredWithSkew(config.clockSkewSeconds())) {
            log.debug("Returning cached access token");
            return cached.accessToken();
        }

        // Single-flight: only one thread refreshes at a time
        refreshLock.lock();
        try {
            // Double-check after acquiring lock
            cached = cache.getIfPresent(CACHE_KEY);
            if (cached != null && !cached.isExpiredWithSkew(config.clockSkewSeconds())) {
                return cached.accessToken();
            }

            log.info("Refreshing access token from {}", config.tokenEndpoint());
            TokenResponse fresh = fetchWithRetry();
            cache.put(CACHE_KEY, fresh);
            return fresh.accessToken();
        } finally {
            refreshLock.unlock();
        }
    }

    private TokenResponse fetchWithRetry() {
        int attempt = 0;
        Exception lastEx = null;
        while (attempt < config.maxRetries()) {
            try {
                String assertion = assertionBuilder.buildAssertion();
                return endpointClient.exchangeAssertion(config.tokenEndpoint(), assertion);
            } catch (Exception ex) {
                lastEx = ex;
                attempt++;
                log.warn("Token fetch attempt {} failed: {}", attempt, ex.getMessage());
                if (attempt < config.maxRetries()) {
                    try {
                        Thread.sleep(500L * attempt);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        throw new IllegalStateException("Failed to obtain token after " + attempt + " attempts", lastEx);
    }
}
