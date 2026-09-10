package com.vocaquiz.catalog;

import com.vocaquiz.catalog.service.AnswerPatternExpander;
import com.vocaquiz.common.error.ApiException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class AnswerPatternExpanderTest {

    @Test
    @DisplayName("완료 기준 — 괄호 둘이 곱해져 6개가 된다")
    void expandsProductOfTwoGroups() {
        assertThat(AnswerPatternExpander.expand("(히토|인간|사람)(마니아|매니아)"))
            .containsExactly(
                "히토마니아", "히토매니아",
                "인간마니아", "인간매니아",
                "사람마니아", "사람매니아");
    }

    @Test
    @DisplayName("완료 기준 — 이스케이프한 괄호와 파이프는 문자로 남는다")
    void escapedSyntaxStaysLiteral() {
        assertThat(AnswerPatternExpander.expand("\\(히토\\|인간\\)"))
            .containsExactly("(히토|인간)");
    }

    @Test
    @DisplayName("빈 대안은 그 자리를 통째로 비운다")
    void emptyAlternativeIsAllowed() {
        assertThat(AnswerPatternExpander.expand("ABC(D||E)"))
            .containsExactly("ABCD", "ABC", "ABCE");
    }

    @Test
    @DisplayName("빈 괄호는 아무것도 더하지 않는다")
    void emptyGroupIsAllowed() {
        assertThat(AnswerPatternExpander.expand("ABC()D")).containsExactly("ABCD");
    }

    @Test
    @DisplayName("괄호는 중첩할 수 없다")
    void rejectsNestedParens() {
        assertThatThrownBy(() -> AnswerPatternExpander.expand("ABC(D|(E|F)G)H"))
            .isInstanceOf(ApiException.class)
            .hasMessageContaining("중첩");
    }

    @Test
    @DisplayName("여러 줄은 줄마다 전개해 합치고, 빈 줄은 버리며, 중복은 남긴다")
    void expandsEveryLine() {
        String pattern = """
            (히토|인간)마니아

            히토마니아
            """;

        assertThat(AnswerPatternExpander.expand(pattern))
            .containsExactly("히토마니아", "인간마니아", "히토마니아");
    }

    @Test
    @DisplayName("countOf는 전개하지 않고 곱을 센다")
    void countsWithoutExpanding() {
        assertThat(AnswerPatternExpander.countOf("(a|b|c)(d|e)\n(f|g)")).isEqualTo(8);
    }

    @Test
    @DisplayName("문법 오류는 빈 줄을 포함한 원본 줄 번호와 글자 위치를 알려 준다")
    void reportsLineAndColumn() {
        assertThatThrownBy(() -> AnswerPatternExpander.expand("정상\n\n(안닫힘"))
            .isInstanceOf(ApiException.class)
            .hasMessageContaining("3번째 줄")
            .hasMessageContaining("1번째 글자");
    }

    @Test
    @DisplayName("괄호 밖의 파이프와 짝 없는 닫는 괄호는 실패한다")
    void rejectsStrayPipeAndParen() {
        assertThatThrownBy(() -> AnswerPatternExpander.expand("A|B"))
            .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> AnswerPatternExpander.expand("A)B"))
            .isInstanceOf(ApiException.class);
    }

    @Test
    @DisplayName("줄이 이스케이프 문자로 끝나면 실패한다")
    void rejectsTrailingBackslash() {
        assertThatThrownBy(() -> AnswerPatternExpander.expand("ABC\\"))
            .isInstanceOf(ApiException.class);
    }

    @Test
    @DisplayName("빈 원문은 빈 결과")
    void emptyPatternProducesNothing() {
        assertThat(AnswerPatternExpander.expand("")).isEmpty();
        assertThat(AnswerPatternExpander.expand("   \n  ")).isEmpty();
        assertThat(AnswerPatternExpander.countOf("")).isZero();
    }
}
