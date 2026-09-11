package com.vocaquiz.catalog.repository;

import com.vocaquiz.catalog.domain.Song;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SongRepository extends JpaRepository<Song, Long> {
}
