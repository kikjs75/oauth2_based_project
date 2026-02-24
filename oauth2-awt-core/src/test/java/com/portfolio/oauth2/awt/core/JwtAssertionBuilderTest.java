package com.portfolio.oauth2.awt.core;

import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.security.*;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JwtAssertionBuilderTest {

    private static String pemKey;

    @BeforeAll
    static void generateKey() throws Exception {
        KeyPair keyPair = KeyPairGenerator.getInstance("RSA").generateKeyPair();
        pemKey = "-----BEGIN PRIVATE KEY-----\n"
                + Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded())
                + "\n-----END PRIVATE KEY-----";
    }

    @Test
    void buildAssertion_google_producesValidSignedJwt() throws Exception {
        AssertionConfig config = AssertionConfig.builder()
                .provider(AssertionConfig.Provider.GOOGLE)
                .clientId("test-client@project.iam.gserviceaccount.com")
                .tokenEndpoint("https://oauth2.googleapis.com/token")
                .scopes(List.of("https://www.googleapis.com/auth/cloud-messaging"))
                .privateKeyPem(pemKey)
                .keyId("key-1")
                .build();

        String jwt = new JwtAssertionBuilder(config).buildAssertion();

        assertNotNull(jwt);
        SignedJWT parsed = SignedJWT.parse(jwt);
        assertEquals("test-client@project.iam.gserviceaccount.com", parsed.getJWTClaimsSet().getIssuer());
        assertEquals("https://www.googleapis.com/auth/cloud-messaging",
                parsed.getJWTClaimsSet().getStringClaim("scope"));
    }

    @Test
    void buildAssertion_microsoft_producesValidSignedJwt_withoutScopeClaim() throws Exception {
        String tenantId = "test-tenant-id";
        String tokenEndpoint = "https://login.microsoftonline.com/" + tenantId + "/oauth2/v2.0/token";

        AssertionConfig config = AssertionConfig.builder()
                .provider(AssertionConfig.Provider.MICROSOFT)
                .clientId("test-azure-client-id")
                .tokenEndpoint(tokenEndpoint)
                .scopes(List.of("https://graph.microsoft.com/.default"))
                .privateKeyPem(pemKey)
                .keyId("cert-thumbprint-1")
                .build();

        String jwt = new JwtAssertionBuilder(config).buildAssertion();

        assertNotNull(jwt);
        SignedJWT parsed = SignedJWT.parse(jwt);
        // iss == sub == clientId (Microsoft JWT assertion 스펙)
        assertEquals("test-azure-client-id", parsed.getJWTClaimsSet().getIssuer());
        assertEquals("test-azure-client-id", parsed.getJWTClaimsSet().getSubject());
        // aud == tokenEndpoint
        assertTrue(parsed.getJWTClaimsSet().getAudience().contains(tokenEndpoint));
        // Microsoft는 JWT에 scope 클레임 미포함 (POST body로 전달)
        assertNull(parsed.getJWTClaimsSet().getStringClaim("scope"));
        // keyId (인증서 지문) 헤더 확인
        assertEquals("cert-thumbprint-1", parsed.getHeader().getKeyID());
    }
}
