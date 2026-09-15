package com.vocaquiz.catalog.repository;

import com.vocaquiz.catalog.domain.SongName;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SongNameRepository extends JpaRepository<SongName, Long> {
}
