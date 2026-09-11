package com.vocaquiz.catalog.repository;

import com.vocaquiz.catalog.domain.SongAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SongAnswerRepository extends JpaRepository<SongAnswer, Long> {

    List<SongAnswer> findBySongId(Long songId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from SongAnswer sa where sa.song.id = :songId")
    void deleteBySongId(@Param("songId") Long songId);
}
