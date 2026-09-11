package com.vocaquiz.catalog.repository;

import com.vocaquiz.catalog.domain.SongVocal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SongVocalRepository extends JpaRepository<SongVocal, Long> {

    List<SongVocal> findBySongId(Long songId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from SongVocal sv where sv.song.id = :songId")
    void deleteBySongId(@Param("songId") Long songId);
}
