package com.vocaquiz.catalog.service;

import com.vocaquiz.catalog.domain.Language;

/** 카탈로그가 언어 하나에 대해 아는 것 (D-070). 서비스가 트랜잭션 안에서 만들어 돌려준다. */
public record LanguageSummary(
    Long id,
    String code,
    String name
) {

    /** 엔티티를 읽는 유일한 지점이라 패키지 밖으로 열지 않는다 (VideoSummary와 같은 이유). */
    static LanguageSummary from(Language language) {
        return new LanguageSummary(language.getId(), language.getCode(), language.getName());
    }
}
