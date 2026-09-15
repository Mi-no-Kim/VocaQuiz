package com.vocaquiz.catalog.repository;

import com.vocaquiz.catalog.domain.SongName;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SongNameRepository extends JpaRepository<SongName, Long> {

    List<SongName> findBySongId(Long songId);

    /** PUBLISHED 전환 조건 1 — 원제 언어의 대표 이름이 있는지 (D-062). */
    boolean existsBySongIdAndLanguageIdAndIsPrimary(Long songId, Long languageId, boolean isPrimary);
}
