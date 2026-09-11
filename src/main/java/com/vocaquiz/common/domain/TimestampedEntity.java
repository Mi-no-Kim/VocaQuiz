package com.vocaquiz.common.domain;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.Instant;

/** 생성 시각과 수정 시각을 함께 갖는 엔티티의 부모. */
@MappedSuperclass
@Getter
public abstract class TimestampedEntity extends CreatedAtEntity {

    @LastModifiedDate
    @Column(nullable = false)
    private Instant updatedAt;
}
