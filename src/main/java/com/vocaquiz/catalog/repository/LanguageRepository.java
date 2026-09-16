package com.vocaquiz.catalog.repository;

import com.vocaquiz.catalog.domain.Language;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LanguageRepository extends JpaRepository<Language, Long> {

    Optional<Language> findByCode(String code);

    /** 참조 목록 화면(D-070의 LanguageService)이 안정적인 순서로 받는다. */
    List<Language> findAllByOrderByCodeAsc();
}
