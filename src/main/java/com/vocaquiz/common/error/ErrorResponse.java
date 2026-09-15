package com.vocaquiz.common.error;

import java.util.List;

public record ErrorResponse(
    String message,
    ErrorCode errorCode,
    List<String> missingConditions
) {

    public ErrorResponse(String message, ErrorCode errorCode) {
        this(message, errorCode, List.of());
    }
}
