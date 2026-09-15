package com.vocaquiz.catalog.repository;

import com.vocaquiz.catalog.domain.SongLanguage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SongLanguageRepository extends JpaRepository<SongLanguage, Long> {
}
