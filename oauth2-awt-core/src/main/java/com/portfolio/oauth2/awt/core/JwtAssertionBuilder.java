package com.portfolio.oauth2.awt.core;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.*;

import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

/**
 * Builds a signed JWT assertion for OAuth2 JWT Bearer flow.
 */
public class JwtAssertionBuilder {

    private final AssertionConfig config;

    public JwtAssertionBuilder(AssertionConfig config) {
        this.config = config;
    }

    public String buildAssertion() {
        try {
            RSAPrivateKey privateKey = loadPrivateKey(config.privateKeyPem());

            Instant now = Instant.now();
            Instant exp = now.plusSeconds(config.tokenExpirySeconds());

            JWTClaimsSet.Builder claimsBuilder = new JWTClaimsSet.Builder()
                    .issuer(config.clientId())
                    .subject(config.clientId())
                    .audience(config.tokenEndpoint())
                    .issueTime(Date.from(now))
                    .expirationTime(Date.from(exp))
                    .jwtID(UUID.randomUUID().toString());

            if (config.provider() == AssertionConfig.Provider.GOOGLE) {
                claimsBuilder.claim("scope", String.join(" ", config.scopes()));
            }

            JWSHeader.Builder headerBuilder = new JWSHeader.Builder(JWSAlgorithm.RS256)
                    .type(JOSEObjectType.JWT);
            if (config.keyId() != null) {
                headerBuilder.keyID(config.keyId());
            }

            SignedJWT signedJWT = new SignedJWT(headerBuilder.build(), claimsBuilder.build());
            signedJWT.sign(new RSASSASigner(privateKey));
            return signedJWT.serialize();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to build JWT assertion", e);
        }
    }

    private RSAPrivateKey loadPrivateKey(String pem) throws Exception {
        String stripped = pem
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace("-----BEGIN RSA PRIVATE KEY-----", "")
                .replace("-----END RSA PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        byte[] decoded = Base64.getDecoder().decode(stripped);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(decoded);
        return (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(spec);
    }
}
