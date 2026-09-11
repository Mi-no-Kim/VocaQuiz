package com.vocaquiz.catalog.service;

import com.vocaquiz.catalog.domain.Language;
import com.vocaquiz.catalog.repository.LanguageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * 언어 시드 (D-036).
 *
 * <p>언어 추가는 스키마 변경이 아니라 행 추가다. 그래서 enum이 아니라 이 시드가
 * 기본값을 넣는다. 없는 것만 넣으므로 몇 번 떠도 같은 결과다 — dev는
 * ddl-auto=update라 데이터가 남아 있고, 테스트는 create-drop이라 매번 새로 넣는다.
 */
@Component
@RequiredArgsConstructor
public class LanguageSeeder implements ApplicationRunner {

    private static final List<Map.Entry<String, String>> SEED = List.of(
        Map.entry("KO", "한국어"),
        Map.entry("EN", "English"),
        Map.entry("JA", "日本語"));

    private final LanguageRepository languageRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        SEED.forEach(entry -> languageRepository.findByCode(entry.getKey())
            .orElseGet(() -> languageRepository.save(Language.create(entry.getKey(), entry.getValue()))));
    }
}
