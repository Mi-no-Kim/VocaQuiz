package com.vocaquiz.catalog.repository;

import com.vocaquiz.catalog.domain.SongName;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SongNameRepository extends JpaRepository<SongName, Long> {

    List<SongName> findBySongId(Long songId);

    /** 관리자 목록에서 여러 곡의 이름을 한 번에 모아 표시 이름을 계산할 때 쓴다 (D-074). */
    List<SongName> findBySongIdIn(List<Long> songIds);

    /** PUBLISHED 전환 조건 1 — 이름이 하나라도 있는지 (D-062, D-072). */
    boolean existsBySongId(Long songId);

    /**
     * 목록 화면의 "미작업" 배치 계산 — 이름이 있는 song id만 골라낸다 (D-062).
     * 곡마다 {@link #existsBySongId}를 부르면 N+1이 되어 한 번에 조회한다.
     */
    @Query("select distinct sn.song.id from SongName sn where sn.song.id in :songIds")
    List<Long> findSongIdsWithNameIn(@Param("songIds") List<Long> songIds);
}
