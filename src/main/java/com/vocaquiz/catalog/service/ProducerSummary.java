package com.vocaquiz.catalog.service;

import com.vocaquiz.catalog.domain.Producer;

/**
 * 카탈로그가 프로듀서 하나에 대해 아는 것 (D-070). 서비스가 트랜잭션 안에서 만들어 돌려준다.
 *
 * <p>지금은 {@code AdminProducerResponse}와 필드가 똑같다. {@link VideoSummary}와 같은
 * 이유로, 둘이 끝내 안 갈라지면 D-070의 단서를 못 지킨 것이다.
 */
public record ProducerSummary(
    Long id,
    String name
) {

    /** 엔티티를 읽는 유일한 지점이라 패키지 밖으로 열지 않는다 (VideoSummary와 같은 이유). */
    static ProducerSummary from(Producer producer) {
        return new ProducerSummary(producer.getId(), producer.getName());
    }
}
