package com.portfolio.fcm;

import com.portfolio.oauth2.awt.core.AssertionTokenClient;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FcmClientTest {

    @Mock
    private RestClient restClient;

    @Mock
    private AssertionTokenClient tokenClient;

    private SimpleMeterRegistry meterRegistry;
    private FcmClient fcmClient;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        FcmConfig config = new FcmConfig("my-project", null);
        fcmClient = new FcmClient(config, tokenClient, restClient, meterRegistry);
        stubRestClientChain();
    }

    /**
     * Spring RestClient 체인은 제네릭 반환 타입(S)을 사용하기 때문에
     * RETURNS_DEEP_STUBS 만으로는 런타임에 타입 추론이 불가능하여 NPE가 발생한다.
     * 따라서 각 단계를 명시적으로 스터빙한다.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void stubRestClientChain() {
        RestClient.RequestBodyUriSpec uriSpec = mock(RestClient.RequestBodyUriSpec.class);
        RestClient.RequestBodySpec bodySpec = mock(RestClient.RequestBodySpec.class);
        RestClient.RequestHeadersSpec headersSpec = mock(RestClient.RequestHeadersSpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

        lenient().given(restClient.post()).willReturn(uriSpec);
        lenient().given(uriSpec.uri(anyString())).willReturn(bodySpec);
        lenient().given(bodySpec.header(anyString(), any(String[].class))).willReturn(bodySpec);
        lenient().given(bodySpec.contentType(any())).willReturn(bodySpec);
        lenient().given(bodySpec.body(any(Object.class))).willReturn(headersSpec);
        lenient().given(headersSpec.retrieve()).willReturn(responseSpec);
        lenient().given(responseSpec.toBodilessEntity()).willReturn(ResponseEntity.ok().build());
    }

    @Test
    @DisplayName("send() → tokenClient.getAccessToken() 호출하여 Bearer 토큰 획득")
    void send_fetchesAccessToken() {
        given(tokenClient.getAccessToken()).willReturn("test-access-token");

        fcmClient.send(new FcmMessage("device-token", "Title", "Body", null));

        verify(tokenClient).getAccessToken();
    }

    @Test
    @DisplayName("send() → RestClient를 통해 FCM API POST 요청")
    void send_postsToFcmApi() {
        given(tokenClient.getAccessToken()).willReturn("test-access-token");

        fcmClient.send(new FcmMessage("device-token", "Title", "Body", null));

        verify(restClient).post();
    }

    @Test
    @DisplayName("send() → Micrometer 타이머 기록 (count=1)")
    void send_recordsTimer() {
        given(tokenClient.getAccessToken()).willReturn("token");

        fcmClient.send(new FcmMessage("device-token", "Title", "Body", null));

        assertThat(meterRegistry.find("fcm.send.duration").timer()).isNotNull();
        assertThat(meterRegistry.find("fcm.send.duration").timer().count()).isEqualTo(1);
    }

    @Test
    @DisplayName("send() 두 번 호출 → 타이머 count=2")
    void send_twice_timerCountIsTwo() {
        given(tokenClient.getAccessToken()).willReturn("token");

        FcmMessage message = new FcmMessage("device-token", "Title", "Body", null);
        fcmClient.send(message);
        fcmClient.send(message);

        assertThat(meterRegistry.find("fcm.send.duration").timer().count()).isEqualTo(2);
    }

    @Test
    @DisplayName("sendEndpoint null → projectId 기반 기본 URL 사용")
    void resolvedSendEndpoint_usesDefaultUrlWhenNotSet() {
        FcmConfig config = new FcmConfig("test-project", null);
        assertThat(config.resolvedSendEndpoint())
                .isEqualTo("https://fcm.googleapis.com/v1/projects/test-project/messages:send");
    }

    @Test
    @DisplayName("sendEndpoint 설정 시 해당 URL 사용")
    void resolvedSendEndpoint_usesCustomUrlWhenSet() {
        FcmConfig config = new FcmConfig("test-project", "http://custom-endpoint/send");
        assertThat(config.resolvedSendEndpoint()).isEqualTo("http://custom-endpoint/send");
    }
}
