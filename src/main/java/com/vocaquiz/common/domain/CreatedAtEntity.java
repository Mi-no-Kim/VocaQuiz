package com.vocaquiz.common.domain;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

/**
 * 생성 시각만 갖는 엔티티의 부모.
 *
 * <p>스키마의 시각 컬럼은 셋으로 갈린다. 절반은 아예 없고, 있는 것도 대부분
 * created_at뿐이다. 부모를 하나로 합치면 파생 테이블인 song_answer에
 * updated_at이 생겨서 고쳐도 되는 것처럼 보인다.
 *
 * <p>리스너는 상속되므로 여기 한 번만 붙인다.
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Getter
public abstract class CreatedAtEntity {

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
}
