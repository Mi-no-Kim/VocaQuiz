package com.vocaquiz.catalog.repository;

import com.vocaquiz.catalog.domain.SongLanguage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SongLanguageRepository extends JpaRepository<SongLanguage, Long> {

    List<SongLanguage> findBySongId(Long songId);

    /** 곡 수정(PUT)에서 통째로 다시 쓴다 — song_language에는 song_name과 달리 행 자체의
     * 식별자로 갱신할 다른 필드가 없다 (B2).
     *
     * <p>파생 delete는 엔티티별 remove가 flush까지 미뤄지는데, 뒤이은 save()는 IDENTITY라
     * 즉시 insert를 내보내 — language_id를 안 바꾸고 그대로 다시 보내면 delete가 반영되기
     * 전에 같은 값으로 insert해 uk_song_language에 걸린다. flushAutomatically로 곧장
     * 반영한다 (SongVocalRepository·VideoVocalRepository와 같은 패턴).
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from SongLanguage sl where sl.song.id = :songId")
    void deleteBySongId(@Param("songId") Long songId);
}
