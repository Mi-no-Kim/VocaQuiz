package com.vocaquiz.common.error;

import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("제약 이름에 uk_가 있으면 409 CONFLICT로 바꾼다")
    void uniqueConstraintViolationBecomesConflict() {
        DataIntegrityViolationException e = wrapping("uk_producer_name");

        ResponseEntity<ErrorResponse> response = handler.handle(e);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().errorCode()).isEqualTo(ErrorCode.CONFLICT);
    }

    @Test
    @DisplayName("제약 이름이 대문자(UK_...)여도 같다 — H2가 따옴표 없는 식별자를 대문자로 돌려주는 경우")
    void uppercaseConstraintNameStillMatches() {
        DataIntegrityViolationException e = wrapping("PRODUCER.UK_PRODUCER_NAME");

        ResponseEntity<ErrorResponse> response = handler.handle(e);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    @DisplayName("uk_가 없는 제약 위반(NOT NULL·FK 등)은 다시 던져 500으로 보낸다")
    void nonUniqueConstraintViolationRethrows() {
        DataIntegrityViolationException e = wrapping("fk_song_credit_producer");

        assertThatThrownBy(() -> handler.handle(e)).isSameAs(e);
    }

    @Test
    @DisplayName("cause가 Hibernate ConstraintViolationException이 아니면 다시 던진다")
    void unknownCauseRethrows() {
        DataIntegrityViolationException e = new DataIntegrityViolationException("boom");

        assertThatThrownBy(() -> handler.handle(e)).isSameAs(e);
    }

    private static DataIntegrityViolationException wrapping(String constraintName) {
        ConstraintViolationException cause =
            new ConstraintViolationException("duplicate", new SQLException("duplicate"), constraintName);
        return new DataIntegrityViolationException("duplicate", cause);
    }
}
