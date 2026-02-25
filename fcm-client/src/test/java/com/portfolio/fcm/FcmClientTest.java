package com.portfolio.fcm;

import com.portfolio.oauth2.awt.core.AssertionTokenClient;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FcmClientTest {

    @Mock
    private RestClient restClient;

    @Mock
    private AssertionTokenClient tokenClient;

    // RestClient chain mocks — fields so verifications can reference them
    private RestClient.RequestBodyUriSpec uriSpec;
    private RestClient.RequestBodySpec bodySpec;
    private RestClient.ResponseSpec responseSpec;

    private SimpleMeterRegistry meterRegistry;
    private FcmClient fcmClient;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        FcmConfig config = new FcmConfig("my-project", null);
        fcmClient = new FcmClient(config, tokenClient, restClient, meterRegistry);
    }

    /**
     * Spring RestClient 체인은 제네릭 반환 타입(S)을 사용하기 때문에
     * RETURNS_DEEP_STUBS 만으로는 런타임에 타입 추론이 불가능하여 NPE가 발생한다.
     * 따라서 각 단계를 명시적으로 스터빙한다.
     * Spring 6에서 body(Object)는 RequestBodySpec을 반환하므로 headersSpec이 필요 없다.
     */
    private void stubRestClientChain() {
        uriSpec = mock(RestClient.RequestBodyUriSpec.class);
        bodySpec = mock(RestClient.RequestBodySpec.class);
        responseSpec = mock(RestClient.ResponseSpec.class);

        given(restClient.post()).willReturn(uriSpec);
        given(uriSpec.uri(anyString())).willReturn(bodySpec);
        given(bodySpec.header(anyString(), (String[]) any())).willReturn(bodySpec);
        given(bodySpec.contentType(any())).willReturn(bodySpec);
        given(bodySpec.body((Object) any())).willReturn(bodySpec);
        given(bodySpec.retrieve()).willReturn(responseSpec);
    }

    @Test
    @DisplayName("send() → Bearer 토큰 획득 후 Authorization 헤더로 FCM API POST 요청")
    void send_invokesRestClientWithBearerToken() {
        stubRestClientChain();
        given(tokenClient.getAccessToken()).willReturn("test-access-token");

        fcmClient.send(new FcmMessage("device-token", "Title", "Body", null));

        verify(tokenClient).getAccessToken();
        verify(restClient).post();
        verify(bodySpec).header(eq(HttpHeaders.AUTHORIZATION), eq("Bearer test-access-token"));
    }

    @Test
    @DisplayName("send() → Micrometer 타이머 기록 (count=1)")
    void send_recordsTimer() {
        stubRestClientChain();
        given(tokenClient.getAccessToken()).willReturn("token");

        fcmClient.send(new FcmMessage("device-token", "Title", "Body", null));

        assertThat(meterRegistry.find("fcm.send.duration").timer()).isNotNull();
        assertThat(meterRegistry.find("fcm.send.duration").timer().count()).isEqualTo(1);
    }

    @Test
    @DisplayName("send() 두 번 호출 → 타이머 count=2")
    void send_twice_timerCountIsTwo() {
        stubRestClientChain();
        given(tokenClient.getAccessToken()).willReturn("token");

        FcmMessage message = new FcmMessage("device-token", "Title", "Body", null);
        fcmClient.send(message);
        fcmClient.send(message);

        assertThat(meterRegistry.find("fcm.send.duration").timer().count()).isEqualTo(2);
    }

    @Test
    @DisplayName("getAccessToken() 예외 → send()에서 그대로 전파")
    void send_whenTokenClientThrows_propagatesException() {
        given(tokenClient.getAccessToken()).willThrow(new RuntimeException("token error"));

        assertThatThrownBy(() -> fcmClient.send(new FcmMessage("device-token", "Title", "Body", null)))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("token error");
    }

    @Test
    @DisplayName("FCM API 오류 응답 → RestClientException 전파")
    void send_whenFcmApiReturnsError_propagatesException() {
        stubRestClientChain();
        given(tokenClient.getAccessToken()).willReturn("token");
        given(responseSpec.toBodilessEntity()).willThrow(new RestClientException("FCM API error"));

        assertThatThrownBy(() -> fcmClient.send(new FcmMessage("device-token", "Title", "Body", null)))
                .isInstanceOf(RestClientException.class)
                .hasMessage("FCM API error");
    }

    @Test
    @DisplayName("null token → FcmMessage 생성 시 NullPointerException")
    void nullToken_throwsNullPointerException() {
        assertThatThrownBy(() -> new FcmMessage(null, "Title", "Body", null))
                .isInstanceOf(NullPointerException.class);
    }
}
