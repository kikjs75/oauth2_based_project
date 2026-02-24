package com.portfolio.app.push;

import com.portfolio.app.push.dto.PushRequest;
import com.portfolio.fcm.FcmClient;
import com.portfolio.fcm.FcmMessage;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class PushService {

    private final Optional<FcmClient> fcmClient;

    public PushService(Optional<FcmClient> fcmClient) {
        this.fcmClient = fcmClient;
    }

    public void sendPush(PushRequest request) {
        FcmClient client = fcmClient.orElseThrow(() ->
                new IllegalStateException("FCM client not configured. Set GOOGLE_SERVICE_ACCOUNT_KEY_PATH."));
        FcmMessage message = new FcmMessage(
                request.deviceToken(),
                request.title(),
                request.body(),
                null
        );
        client.send(message);
    }
}
