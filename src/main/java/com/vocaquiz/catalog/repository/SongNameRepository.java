package com.vocaquiz.catalog.repository;

import com.vocaquiz.catalog.domain.SongName;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SongNameRepository extends JpaRepository<SongName, Long> {

    List<SongName> findBySongId(Long songId);

    /** PUBLISHED 전환 조건 1 — 이름이 하나라도 있는지 (D-062, D-072). */
    boolean existsBySongId(Long songId);
}
