package com.vocaquiz.catalog.repository;

import com.vocaquiz.catalog.domain.Producer;
import org.springframework.data.jpa.repository.JpaRepository;

/** 이름은 이제 {@code producer} 자신의 컬럼이 아니다 — {@link ProducerNameRepository}가 맡는다 (D-073). */
public interface ProducerRepository extends JpaRepository<Producer, Long> {
}
