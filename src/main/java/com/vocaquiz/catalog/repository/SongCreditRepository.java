package com.vocaquiz.catalog.repository;

import com.vocaquiz.catalog.domain.SongCredit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SongCreditRepository extends JpaRepository<SongCredit, Long> {

    List<SongCredit> findBySongId(Long songId);

    /** 곡 수정(PUT)에서 통째로 다시 쓴다 — song_language와 같은 이유 (B2), 같은 이유로
     * flushAutomatically가 필요하다 — {@link SongLanguageRepository#deleteBySongId} 참고. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from SongCredit sc where sc.song.id = :songId")
    void deleteBySongId(@Param("songId") Long songId);
}
