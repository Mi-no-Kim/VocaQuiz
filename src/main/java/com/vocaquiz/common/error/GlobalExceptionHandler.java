package com.vocaquiz.common.error;

import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
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
@Slf4j
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

    /**
     * 사전검사(예: {@code findByName} 후 {@code save})와 실제 저장 사이의 레이스로, UNIQUE
     * 제약을 DB가 대신 막아 준 경우를 여기서 받는다 (사전검사 자체가 잡는 경우는
     * {@link ApiException}으로 먼저 던져져 이 핸들러까지 안 온다).
     *
     * <p>제약 이름에 "uk_"가 없으면(NOT NULL·FK 등 다른 무결성 위반) 다시 던져 500으로
     * 보낸다 — 진짜 버그를 CONFLICT 뒤에 숨기지 않기 위해서다. "uk_" 접두어는 관례가 아니라
     * 계약이다 — 새 UNIQUE 제약을 만들 때 반드시 지켜야 한다 (04-SCHEMA.md 0절).
     *
     * <p>{@code getConstraintName()}의 대소문자는 DB 방언마다 다르다 — H2는 따옴표 없는
     * 식별자를 대문자로 돌려준다 — 그래서 소문자로 정규화해서 비교한다.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ErrorResponse> handle(DataIntegrityViolationException e) {
        if (e.getCause() instanceof ConstraintViolationException cve
                && cve.getConstraintName() != null
                && cve.getConstraintName().toLowerCase().contains("uk_")) {
            log.warn("UNIQUE 제약 위반: {}", cve.getConstraintName());
            return ResponseEntity
                .status(ErrorCode.CONFLICT.status())
                .body(new ErrorResponse("이미 등록된 데이터와 충돌했다", ErrorCode.CONFLICT));
        }
        throw e;
    }

    private static String defaultMessageOf(FieldError error) {
        return error.getDefaultMessage() == null ? "값이 올바르지 않습니다" : error.getDefaultMessage();
    }
}
