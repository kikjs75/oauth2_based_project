package com.portfolio.oauth2.awt.core;

import java.util.List;

/**
 * Configuration for an OAuth2 JWT assertion client (Google or Microsoft).
 */
public record AssertionConfig(
        Provider provider,
        String clientId,
        String tokenEndpoint,
        List<String> scopes,
        String privateKeyPem,
        String keyId,
        long clockSkewSeconds,
        long tokenExpirySeconds,
        int maxRetries,
        long timeoutMs
) {
    public enum Provider { GOOGLE, MICROSOFT }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Provider provider;
        private String clientId;
        private String tokenEndpoint;
        private List<String> scopes;
        private String privateKeyPem;
        private String keyId;
        private long clockSkewSeconds = 60;
        private long tokenExpirySeconds = 3600;
        private int maxRetries = 3;
        private long timeoutMs = 10_000;

        public Builder provider(Provider p) { this.provider = p; return this; }
        public Builder clientId(String s) { this.clientId = s; return this; }
        public Builder tokenEndpoint(String s) { this.tokenEndpoint = s; return this; }
        public Builder scopes(List<String> s) { this.scopes = s; return this; }
        public Builder privateKeyPem(String s) { this.privateKeyPem = s; return this; }
        public Builder keyId(String s) { this.keyId = s; return this; }
        public Builder clockSkewSeconds(long v) { this.clockSkewSeconds = v; return this; }
        public Builder tokenExpirySeconds(long v) { this.tokenExpirySeconds = v; return this; }
        public Builder maxRetries(int v) { this.maxRetries = v; return this; }
        public Builder timeoutMs(long v) { this.timeoutMs = v; return this; }

        public AssertionConfig build() {
            return new AssertionConfig(provider, clientId, tokenEndpoint, scopes,
                    privateKeyPem, keyId, clockSkewSeconds, tokenExpirySeconds, maxRetries, timeoutMs);
        }
    }
}
