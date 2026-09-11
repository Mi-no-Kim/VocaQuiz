package com.vocaquiz.catalog.repository;

import com.vocaquiz.catalog.domain.Video;
import com.vocaquiz.catalog.domain.VideoKind;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VideoRepository extends JpaRepository<Video, Long> {

    List<Video> findBySongIdAndKind(Long songId, VideoKind kind);
}
