package com.portfolio.app.push;

import com.portfolio.app.push.dto.PushRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/push")
public class PushController {

    private final PushService pushService;

    public PushController(PushService pushService) {
        this.pushService = pushService;
    }

    @PostMapping("/test")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('WRITER', 'ADMIN')")
    public void sendTestPush(@Valid @RequestBody PushRequest request) {
        pushService.sendPush(request);
    }
}
