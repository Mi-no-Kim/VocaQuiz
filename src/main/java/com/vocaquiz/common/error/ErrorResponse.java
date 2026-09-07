package com.vocaquiz.common.error;

public record ErrorResponse(
    String message,
    ErrorCode errorCode
) {
}
