package com.vocaquiz.catalog.repository;

import com.vocaquiz.catalog.domain.Channel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ChannelRepository extends JpaRepository<Channel, Long> {

    Optional<Channel> findByYoutubeChannelId(String youtubeChannelId);
}
