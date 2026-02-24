package com.portfolio.oauth2.awt.core;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.security.KeyPairGenerator;
import java.security.KeyPair;
import java.time.Instant;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CachedTokenProviderTest {

    private static String pemKey;

    @BeforeAll
    static void generateKey() throws Exception {
        KeyPair keyPair = KeyPairGenerator.getInstance("RSA").generateKeyPair();
        pemKey = "-----BEGIN PRIVATE KEY-----\n"
                + Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded())
                + "\n-----END PRIVATE KEY-----";
    }

    @Test
    void getAccessToken_google_cacheHit_doesNotRefetch() throws Exception {
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

        TokenEndpointClient mockClient = mock(TokenEndpointClient.class);
        TokenResponse fakeToken = new TokenResponse("google-fake-token", Instant.now().plusSeconds(3600));
        when(mockClient.exchangeAssertion(anyString(), anyString())).thenReturn(fakeToken);

        CachedTokenProvider provider = new CachedTokenProvider(config, new JwtAssertionBuilder(config), mockClient);

        assertEquals("google-fake-token", provider.getAccessToken());
        assertEquals("google-fake-token", provider.getAccessToken());
        verify(mockClient, times(1)).exchangeAssertion(anyString(), anyString());
    }

    @Test
    void getAccessToken_microsoft_cacheHit_doesNotRefetch() throws Exception {
        String tenantId = "test-tenant-id";
        AssertionConfig config = AssertionConfig.builder()
                .provider(AssertionConfig.Provider.MICROSOFT)
                .clientId("test-azure-client-id")
                .tokenEndpoint("https://login.microsoftonline.com/" + tenantId + "/oauth2/v2.0/token")
                .scopes(List.of("https://graph.microsoft.com/.default"))
                .privateKeyPem(pemKey)
                .tokenExpirySeconds(3600)
                .clockSkewSeconds(60)
                .maxRetries(3)
                .build();

        TokenEndpointClient mockClient = mock(TokenEndpointClient.class);
        TokenResponse fakeToken = new TokenResponse("microsoft-fake-token", Instant.now().plusSeconds(3600));
        when(mockClient.exchangeAssertion(anyString(), anyString())).thenReturn(fakeToken);

        CachedTokenProvider provider = new CachedTokenProvider(config, new JwtAssertionBuilder(config), mockClient);

        assertEquals("microsoft-fake-token", provider.getAccessToken());
        assertEquals("microsoft-fake-token", provider.getAccessToken());
        verify(mockClient, times(1)).exchangeAssertion(anyString(), anyString());
    }
}
