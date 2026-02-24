package com.portfolio.app.push;

import com.portfolio.app.push.dto.PushRequest;
import com.portfolio.fcm.FcmClient;
import com.portfolio.fcm.FcmMessage;
import org.springframework.stereotype.Service;

@Service
public class PushService {

    private final FcmClient fcmClient;

    public PushService(FcmClient fcmClient) {
        this.fcmClient = fcmClient;
    }

    public void sendPush(PushRequest request) {
        FcmMessage message = new FcmMessage(
                request.deviceToken(),
                request.title(),
                request.body(),
                null
        );
        fcmClient.send(message);
    }
}
