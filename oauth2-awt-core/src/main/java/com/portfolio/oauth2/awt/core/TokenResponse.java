package com.portfolio.oauth2.awt.core;

import java.time.Instant;

public record TokenResponse(String accessToken, Instant expiresAt) {
    public boolean isExpiredWithSkew(long skewSeconds) {
        return Instant.now().isAfter(expiresAt.minusSeconds(skewSeconds));
    }
}
