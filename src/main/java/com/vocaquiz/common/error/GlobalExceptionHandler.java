package com.vocaquiz.common.error;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * 실패 응답의 모양을 {@link ErrorResponse} 하나로 맞춘다.
 *
 * <p>프론트가 {@code errorCode}로 분기하려면(같은 409여도 CONFLICT와
 * COLLECTION_IN_PROGRESS가 다르다) 모든 실패가 같은 본문으로 와야 한다. 우리가 던지는
 * {@code ApiException}뿐 아니라 {@code @Valid} 실패도 여기서 받는 이유다 — 그걸 빼면
 * 요청 본문 검증 실패만 Spring 기본 본문으로 나가 모양이 갈린다.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ErrorResponse> handle(ApiException e) {
        return ResponseEntity
            .status(e.getErrorCode().status())
            .body(new ErrorResponse(e.getMessage(), e.getErrorCode()));
    }

    /** {@code @RequestBody @Valid} 검증 실패. 어느 필드가 왜 틀렸는지를 메시지에 담는다. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorResponse> handle(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
            .map(error -> error.getField() + ": " + defaultMessageOf(error))
            .collect(Collectors.joining(", "));

        return ResponseEntity
            .status(ErrorCode.INVALID_REQUEST.status())
            .body(new ErrorResponse(message, ErrorCode.INVALID_REQUEST));
    }

    private static String defaultMessageOf(FieldError error) {
        return error.getDefaultMessage() == null ? "값이 올바르지 않습니다" : error.getDefaultMessage();
    }
}
