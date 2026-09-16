package com.vocaquiz.catalog.repository;

import com.vocaquiz.catalog.domain.Producer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProducerRepository extends JpaRepository<Producer, Long> {

    Optional<Producer> findByName(String name);

    /**
     * 자동완성 검색 (D-063). DB 콜레이션은 utf8mb4_bin이라 UNIQUE는 대소문자를 구분하지만
     * (D-057), 검색까지 그러면 표기 흔들림으로 인한 중복을 막는 자동완성의 역할을 못 한다.
     * 그래서 여기서는 대소문자를 구분하지 않는다 — "deco"를 쳐도 "DECO*27"이 보여야 한다.
     *
     * <p>{@code pattern}은 {@code %}로 감싸고 리터럴 {@code %}·{@code _}를 이스케이프까지
     * 마친 상태로 호출자({@link com.vocaquiz.catalog.service.ProducerService})가 넘긴다 —
     * 그래야 프로듀서 이름에 저 문자가 실제로 들어 있어도 와일드카드로 오작동하지 않는다.
     */
    @Query("select p from Producer p where lower(p.name) like lower(:pattern) escape '\\' order by p.name asc")
    List<Producer> searchByName(@Param("pattern") String pattern);
}
