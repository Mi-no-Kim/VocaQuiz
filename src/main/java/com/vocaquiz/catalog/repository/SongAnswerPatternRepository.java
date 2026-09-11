package com.vocaquiz.catalog.repository;

import com.vocaquiz.catalog.domain.SongAnswerPattern;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SongAnswerPatternRepository extends JpaRepository<SongAnswerPattern, Long> {

    Optional<SongAnswerPattern> findBySongId(Long songId);
}
