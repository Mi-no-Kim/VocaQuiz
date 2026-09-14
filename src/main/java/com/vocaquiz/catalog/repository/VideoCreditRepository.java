package com.vocaquiz.catalog.repository;

import com.vocaquiz.catalog.domain.VideoCredit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VideoCreditRepository extends JpaRepository<VideoCredit, Long> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from VideoCredit vc where vc.video.id = :videoId")
    void deleteByVideoId(@Param("videoId") Long videoId);
}
