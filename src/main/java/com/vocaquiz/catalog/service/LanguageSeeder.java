package com.vocaquiz.catalog.service;

import com.vocaquiz.catalog.domain.Language;
import com.vocaquiz.catalog.repository.LanguageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 언어 시드 (D-036, D-071).
 *
 * <p>언어 추가는 스키마 변경이 아니라 행 추가다. 그래서 enum이 아니라 이 시드가
 * 기본값을 넣는다. 없는 것만 넣으므로 몇 번 떠도 같은 결과다 — dev는
 * ddl-auto=update라 데이터가 남아 있고, 테스트는 create-drop이라 매번 새로 넣는다.
 *
 * <p>{@code displayOrder}는 표시 폴백 순서다 (D-071) — EN을 0으로 둬서 "없으면 English"가
 * 이 순서의 첫 항목이 되게 했다. KO·JA 사이의 순서는 정해진 기준이 없어 시드 나열 순서를 그대로 썼다.
 */
@Component
@RequiredArgsConstructor
public class LanguageSeeder implements ApplicationRunner {

    private record SeedLanguage(String code, String name, int displayOrder) {
    }

    private static final List<SeedLanguage> SEED = List.of(
        new SeedLanguage("EN", "English", 0),
        new SeedLanguage("KO", "한국어", 1),
        new SeedLanguage("JA", "日本語", 2));

    private final LanguageRepository languageRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        SEED.forEach(seed -> languageRepository.findByCode(seed.code())
            .orElseGet(() -> languageRepository.save(
                Language.create(seed.code(), seed.name(), seed.displayOrder()))));
    }
}
