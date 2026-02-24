package com.portfolio.app.push.dto;

import jakarta.validation.constraints.NotBlank;

public record PushRequest(
        @NotBlank String deviceToken,
        @NotBlank String title,
        @NotBlank String body
) {}
