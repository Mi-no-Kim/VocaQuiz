package com.vocaquiz.catalog;

import com.vocaquiz.catalog.service.TextNormalizer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TextNormalizerTest {

    @Test
    @DisplayName("완료 기준 — 세 언어 표기가 각각 정규화된다")
    void normalizesThreeScripts() {
        assertThat(TextNormalizer.normalize("千本桜")).isEqualTo("千本桜");
        assertThat(TextNormalizer.normalize("Senbonzakura")).isEqualTo("senbonzakura");
        assertThat(TextNormalizer.normalize("천본앵")).isEqualTo("천본앵");
    }

    @Test
    @DisplayName("전각 영숫자는 반각이 된다")
    void nfkcFoldsFullwidth() {
        assertThat(TextNormalizer.normalize("ＭＩＫＵ")).isEqualTo("miku");
    }

    @Test
    @DisplayName("반각 가나는 전각 가나가 된다")
    void nfkcFoldsHalfwidthKana() {
        assertThat(TextNormalizer.normalize("ﾐｸ")).isEqualTo("ミク");
    }

    @Test
    @DisplayName("괄호는 지우되 안의 내용은 남는다")
    void keepsTextInsideBrackets() {
        assertThat(TextNormalizer.normalize("千本桜 (feat. 初音ミク)"))
            .isEqualTo("千本桜feat初音ミク");
        assertThat(TextNormalizer.normalize("【初音ミク】マインドブランド"))
            .isEqualTo("初音ミクマインドブランド");
    }

    @Test
    @DisplayName("가타카나와 히라가나는 통일하지 않는다")
    void doesNotFoldKana() {
        assertThat(TextNormalizer.normalize("ミク"))
            .isNotEqualTo(TextNormalizer.normalize("みく"));
    }

    @Test
    @DisplayName("null은 빈 문자열")
    void nullBecomesEmpty() {
        assertThat(TextNormalizer.normalize(null)).isEmpty();
    }
}
