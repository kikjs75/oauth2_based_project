package com.portfolio.app.push;

import com.portfolio.app.push.dto.PushRequest;
import com.portfolio.fcm.FcmClient;
import com.portfolio.fcm.FcmMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PushServiceTest {

    @Mock
    private FcmClient fcmClient;

    @Test
    @DisplayName("FCM 클라이언트가 설정된 경우 푸시 전송 성공")
    void sendPush_configured_delegatesToFcmClient() {
        PushService service = new PushService(Optional.of(fcmClient));
        PushRequest request = new PushRequest("device-token", "Title", "Body");

        service.sendPush(request);

        verify(fcmClient).send(any(FcmMessage.class));
    }

    @Test
    @DisplayName("FCM 메시지에 요청 내용이 올바르게 매핑됨")
    void sendPush_mapsRequestToFcmMessage() {
        PushService service = new PushService(Optional.of(fcmClient));
        PushRequest request = new PushRequest("my-device", "Hello", "World");

        service.sendPush(request);

        verify(fcmClient).send(new FcmMessage("my-device", "Hello", "World", null));
    }

    @Test
    @DisplayName("FCM 클라이언트 미설정 시 예외")
    void sendPush_notConfigured_throwsException() {
        PushService service = new PushService(Optional.empty());
        PushRequest request = new PushRequest("device-token", "Title", "Body");

        assertThatThrownBy(() -> service.sendPush(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("FCM client not configured");
    }
}
