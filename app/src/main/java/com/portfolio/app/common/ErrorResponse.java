package com.portfolio.app.common;

public record ErrorResponse(String errorCode, String message, String traceId) {}
