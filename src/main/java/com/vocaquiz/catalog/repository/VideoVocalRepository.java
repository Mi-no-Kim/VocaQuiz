package com.vocaquiz.catalog.repository;

import com.vocaquiz.catalog.domain.VideoVocal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface VideoVocalRepository extends JpaRepository<VideoVocal, Long> {

    List<VideoVocal> findByVideoIdIn(Collection<Long> videoIds);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from VideoVocal vv where vv.video.id = :videoId")
    void deleteByVideoId(@Param("videoId") Long videoId);
}
