package com.vocaquiz.catalog.repository;

import com.vocaquiz.catalog.domain.Language;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LanguageRepository extends JpaRepository<Language, Long> {

    Optional<Language> findByCode(String code);
}
