package com.vocaquiz.catalog.repository;

import com.vocaquiz.catalog.domain.ProducerName;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProducerNameRepository extends JpaRepository<ProducerName, Long> {

    List<ProducerName> findByProducerId(Long producerId);

    /**
     * 자동완성 검색 (D-063, D-073). 대소문자를 구분하지 않는다 — {@link
     * com.vocaquiz.catalog.repository.ProducerRepository}의 옛 {@code searchByName}과 같은
     * 이유(D-057의 utf8mb4_bin 비교로는 자동완성의 중복 방지 역할을 못 한다).
     *
     * <p>{@code pattern}은 {@code %}로 감싸고 리터럴 {@code %}·{@code _}를 이스케이프까지
     * 마친 상태로 호출자({@link com.vocaquiz.catalog.service.ProducerService})가 넘긴다.
     *
     * <p>언어 구분 없이 모든 언어의 이름을 다 뒤진다 — 관리자가 어느 언어로 검색하든 그
     * 프로듀서를 찾을 수 있어야 자동완성의 중복 방지 역할(D-063)이 유지된다.
     */
    @Query("select distinct pn.producer.id from ProducerName pn "
        + "where lower(pn.name) like lower(:pattern) escape '\\' order by pn.producer.id asc")
    List<Long> searchProducerIdsByName(@Param("pattern") String pattern);

    /** 같은 언어에 같은 표기가 이미 있는지 (D-073 — UNIQUE 범위는 producer 전체가 아니라 언어별). */
    boolean existsByLanguageIdAndName(Long languageId, String name);
}
