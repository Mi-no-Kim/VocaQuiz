package com.vocaquiz.catalog.repository;

import com.vocaquiz.catalog.domain.SongAnswerPattern;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SongAnswerPatternRepository extends JpaRepository<SongAnswerPattern, Long> {

    Optional<SongAnswerPattern> findBySongId(Long songId);

    /**
     * 목록 화면의 "미작업" 배치 계산 — 정답 패턴이 있는 song id만 골라낸다 (D-056, D-062).
     * 곡마다 {@link #findBySongId}를 부르면 N+1이 되어 한 번에 조회한다.
     */
    @Query("select sap.song.id from SongAnswerPattern sap where sap.song.id in :songIds")
    List<Long> findSongIdsWithPatternIn(@Param("songIds") List<Long> songIds);
}
