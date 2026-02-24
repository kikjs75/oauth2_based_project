package com.portfolio.oauth2.awt.core;

import org.junit.jupiter.api.Test;

import java.security.KeyPairGenerator;
import java.security.KeyPair;
import java.time.Instant;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CachedTokenProviderTest {

    @Test
    void getAccessToken_cacheHit_doesNotRefetch() throws Exception {
        KeyPair keyPair = KeyPairGenerator.getInstance("RSA").generateKeyPair();
        String pemKey = "-----BEGIN PRIVATE KEY-----\n"
                + Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded())
                + "\n-----END PRIVATE KEY-----";

        AssertionConfig config = AssertionConfig.builder()
                .provider(AssertionConfig.Provider.GOOGLE)
                .clientId("test@project.iam.gserviceaccount.com")
                .tokenEndpoint("https://oauth2.googleapis.com/token")
                .scopes(List.of("https://www.googleapis.com/auth/cloud-messaging"))
                .privateKeyPem(pemKey)
                .tokenExpirySeconds(3600)
                .clockSkewSeconds(60)
                .maxRetries(3)
                .build();

        JwtAssertionBuilder assertionBuilder = new JwtAssertionBuilder(config);
        TokenEndpointClient mockClient = mock(TokenEndpointClient.class);
        TokenResponse fakeToken = new TokenResponse("fake-token", Instant.now().plusSeconds(3600));
        when(mockClient.exchangeAssertion(anyString(), anyString())).thenReturn(fakeToken);

        CachedTokenProvider provider = new CachedTokenProvider(config, assertionBuilder, mockClient);

        String token1 = provider.getAccessToken();
        String token2 = provider.getAccessToken();

        assertEquals("fake-token", token1);
        assertEquals("fake-token", token2);
        // Only fetched once due to cache
        verify(mockClient, times(1)).exchangeAssertion(anyString(), anyString());
    }
}
