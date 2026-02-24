package com.portfolio.oauth2.awt.core;

import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.Test;

import java.security.*;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JwtAssertionBuilderTest {

    @Test
    void buildAssertion_producesValidSignedJwt() throws Exception {
        KeyPair keyPair = KeyPairGenerator.getInstance("RSA").generateKeyPair();
        String pemKey = "-----BEGIN PRIVATE KEY-----\n"
                + Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded())
                + "\n-----END PRIVATE KEY-----";

        AssertionConfig config = AssertionConfig.builder()
                .provider(AssertionConfig.Provider.GOOGLE)
                .clientId("test-client@project.iam.gserviceaccount.com")
                .tokenEndpoint("https://oauth2.googleapis.com/token")
                .scopes(List.of("https://www.googleapis.com/auth/cloud-messaging"))
                .privateKeyPem(pemKey)
                .keyId("key-1")
                .build();

        JwtAssertionBuilder builder = new JwtAssertionBuilder(config);
        String jwt = builder.buildAssertion();

        assertNotNull(jwt);
        SignedJWT parsed = SignedJWT.parse(jwt);
        assertEquals("test-client@project.iam.gserviceaccount.com", parsed.getJWTClaimsSet().getIssuer());
        assertEquals("https://www.googleapis.com/auth/cloud-messaging",
                parsed.getJWTClaimsSet().getStringClaim("scope"));
    }
}
