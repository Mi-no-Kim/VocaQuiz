package com.vocaquiz.common.error;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    NOT_FOUND(HttpStatus.NOT_FOUND),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST),
    FORBIDDEN(HttpStatus.FORBIDDEN),
    EXTERNAL_API_ERROR(HttpStatus.BAD_GATEWAY),
    /** 이미 등록된 자원과 충돌한다 (예: 이미 있는 youtubeVideoId, P1-3-3 결정). */
    CONFLICT(HttpStatus.CONFLICT),
    /** 수동/스케줄 수집이 이미 도는 중이다 (P1-3-3 결정 3). */
    COLLECTION_IN_PROGRESS(HttpStatus.CONFLICT);

    private final HttpStatus status;

    ErrorCode(HttpStatus status) {
        this.status = status;
    }

    public HttpStatus status() {
        return status;
    }
}
