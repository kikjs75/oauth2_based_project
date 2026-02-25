package com.portfolio.app.security;

import com.nimbusds.jwt.JWTClaimsSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        // Secret must be at least 256 bits (32 bytes) for HS256
        JwtProperties props = new JwtProperties(
                "test-secret-key-that-is-at-least-32-bytes-long!",
                3_600_000L  // 1 hour
        );
        jwtTokenProvider = new JwtTokenProvider(props);
    }

    @Test
    @DisplayName("generateToken → parseToken 라운드트립 성공")
    void generateAndParse_roundTrip() throws Exception {
        String token = jwtTokenProvider.generateToken(1L, "user@example.com", List.of("ROLE_USER", "ROLE_WRITER"));

        JWTClaimsSet claims = jwtTokenProvider.parseToken(token);

        assertThat(claims.getSubject()).isEqualTo("1");
        assertThat(claims.getStringClaim("username")).isEqualTo("user@example.com");
        assertThat(claims.getStringListClaim("roles")).containsExactlyInAnyOrder("ROLE_USER", "ROLE_WRITER");
    }

    @Test
    @DisplayName("만료된 토큰 → parseToken 예외 발생")
    void parseToken_expired_throwsException() {
        JwtProperties expiredProps = new JwtProperties(
                "test-secret-key-that-is-at-least-32-bytes-long!",
                -1L  // already expired
        );
        JwtTokenProvider expiredProvider = new JwtTokenProvider(expiredProps);
        String expiredToken = expiredProvider.generateToken(1L, "user", List.of("ROLE_USER"));

        assertThatThrownBy(() -> jwtTokenProvider.parseToken(expiredToken))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("JWT");
    }

    @Test
    @DisplayName("잘못된 서명의 토큰 → parseToken 예외 발생")
    void parseToken_wrongSignature_throwsException() {
        JwtProperties otherProps = new JwtProperties(
                "other-secret-key-that-is-at-least-32-bytes-long!!",
                3_600_000L
        );
        JwtTokenProvider otherProvider = new JwtTokenProvider(otherProps);
        String tokenWithOtherKey = otherProvider.generateToken(1L, "user", List.of("ROLE_USER"));

        assertThatThrownBy(() -> jwtTokenProvider.parseToken(tokenWithOtherKey))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("완전히 무효한 문자열 → parseToken 예외 발생")
    void parseToken_garbage_throwsException() {
        assertThatThrownBy(() -> jwtTokenProvider.parseToken("not.a.jwt"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
