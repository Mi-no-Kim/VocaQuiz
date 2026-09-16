package com.vocaquiz.admin.api.dto;

import com.vocaquiz.catalog.service.AnswerPatternExpander;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CheckAnswerPatternResponseTest {

    @Test
    @DisplayName("완료 기준 2 — (히토|인간|사람)(마니아|매니아)는 6개, 경고 없음")
    void sixCombinationsNoWarning() {
        CheckAnswerPatternResponse response =
            CheckAnswerPatternResponse.of(AnswerPatternExpander.expand("(히토|인간|사람)(마니아|매니아)"));

        assertThat(response.results()).hasSize(6);
        assertThat(response.count()).isEqualTo(6);
        assertThat(response.warning()).isFalse();
    }

    @Test
    @DisplayName("완료 기준 2 — (a|b|c)(d|e|f)(g|h|i)는 27개, 경고 있음")
    void twentySevenCombinationsWarns() {
        CheckAnswerPatternResponse response =
            CheckAnswerPatternResponse.of(AnswerPatternExpander.expand("(a|b|c)(d|e|f)(g|h|i)"));

        assertThat(response.results()).hasSize(27);
        assertThat(response.count()).isEqualTo(27);
        assertThat(response.warning()).isTrue();
    }
}
