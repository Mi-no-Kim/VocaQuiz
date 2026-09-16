package com.vocaquiz.catalog.service;

import com.vocaquiz.catalog.repository.LanguageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 언어 참조 목록 (D-036). 곡 이름·언어(D-040·D-051)와 보컬 이름(D-041)이 이 테이블을 공유한다.
 *
 * <p>조회 하나뿐인 서비스지만, 컨트롤러가 리포지토리를 직접 부르지 않는다는 규칙에는
 * 조회만 하는 경우도 예외가 없다 (D-070).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LanguageService {

    private final LanguageRepository languageRepository;

    public List<LanguageSummary> list() {
        return languageRepository.findAllByOrderByCodeAsc().stream()
            .map(LanguageSummary::from)
            .toList();
    }
}
