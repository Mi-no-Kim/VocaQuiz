package com.vocaquiz.common.error;

import lombok.Getter;

import java.util.List;

@Getter
public class ApiException extends RuntimeException {

    private final ErrorCode errorCode;
    private final List<String> missingConditions;

    public ApiException(ErrorCode errorCode) {
        this(errorCode, errorCode.name());
    }

    public ApiException(ErrorCode errorCode, String message) {
        this(errorCode, message, List.of());
    }

    /** D-062처럼 실패 이유가 여러 개일 수 있을 때 — {@link ErrorResponse#missingConditions()}로 그대로 나간다. */
    public ApiException(ErrorCode errorCode, String message, List<String> missingConditions) {
        super(message);
        this.errorCode = errorCode;
        this.missingConditions = missingConditions;
    }
}
