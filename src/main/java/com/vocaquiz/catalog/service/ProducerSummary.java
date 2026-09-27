package com.vocaquiz.catalog.service;

import com.vocaquiz.catalog.domain.ProducerName;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 카탈로그가 프로듀서 하나에 대해 아는 것 (D-070, D-073, D-074). 이름은 언어별로 여러 개라
 * 전부 담아 소비자(관리자 화면)에게 넘기고, 그중 표시할 값(main/sub 표기법, D-074)도
 * 서비스가 트랜잭션 안에서 미리 계산해 같이 준다.
 */
public record ProducerSummary(
    Long id,
    List<ProducerNameSummary> names,
    ResolvedName displayName
) {

    /** 엔티티를 읽는 유일한 지점이라 패키지 밖으로 열지 않는다 (VideoSummary와 같은 이유). */
    static ProducerSummary of(Long producerId, List<ProducerName> names, NameDisplayResolver resolver, NamePreference preference) {
        Map<Long, String> namesByLanguageId = names.stream()
            .collect(Collectors.toMap(
                pn -> pn.getLanguage().getId(), ProducerName::getName, (a, b) -> a));

        return new ProducerSummary(
            producerId,
            names.stream().map(ProducerNameSummary::from).toList(),
            resolver.resolve(namesByLanguageId, preference));
    }

    public record ProducerNameSummary(Long languageId, String name, boolean primary) {

        static ProducerNameSummary from(ProducerName producerName) {
            return new ProducerNameSummary(
                producerName.getLanguage().getId(),
                producerName.getName(),
                producerName.isPrimary());
        }
    }
}
