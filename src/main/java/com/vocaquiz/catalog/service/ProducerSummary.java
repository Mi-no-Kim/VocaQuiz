package com.vocaquiz.catalog.service;

import com.vocaquiz.catalog.domain.ProducerName;

import java.util.List;

/**
 * 카탈로그가 프로듀서 하나에 대해 아는 것 (D-070, D-073). 이름은 이제 언어별로 여러 개다 —
 * 어느 언어를 보여줄지는 이 record가 정하지 않고, 전부 담아 소비자(관리자 화면)에게 넘긴다.
 * {@code Producer} 엔티티 자신은 이제 id밖에 없어 엔티티를 따로 받지 않는다. 서비스가
 * 트랜잭션 안에서 만들어 돌려준다.
 */
public record ProducerSummary(
    Long id,
    List<ProducerNameSummary> names
) {

    /** 엔티티를 읽는 유일한 지점이라 패키지 밖으로 열지 않는다 (VideoSummary와 같은 이유). */
    static ProducerSummary of(Long producerId, List<ProducerName> names) {
        return new ProducerSummary(
            producerId,
            names.stream().map(ProducerNameSummary::from).toList());
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
