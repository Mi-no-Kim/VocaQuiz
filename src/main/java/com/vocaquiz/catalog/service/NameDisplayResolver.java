package com.vocaquiz.catalog.service;

import com.vocaquiz.catalog.domain.Language;
import com.vocaquiz.catalog.repository.LanguageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 곡명·작곡가명 표시값을 main/sub 표기법(D-074)으로 정한다. 곡명·작곡가명 양쪽에서
 * 같은 규칙을 쓴다 — 대상이 무엇인지는 모르고, 언어별 후보 텍스트만 받는다.
 *
 * <p>규칙:
 * <ol>
 *   <li>main 값 == sub 값, 또는 sub 언어가 없거나 그 언어 값이 없다 → main 값만
 *   <li>main 값 != sub 값(둘 다 있고 다르다) → 둘 다(주 표시 · 부 표시로 구분해 보여주는 것은 화면 몫)
 *   <li>main 값이 없다 → sub 값
 *   <li>둘 다 없다 → {@code language.displayOrder} 오름차순으로 값이 있는 첫 언어
 * </ol>
 */
@Component
@RequiredArgsConstructor
public class NameDisplayResolver {

    private final LanguageRepository languageRepository;

    public ResolvedName resolve(Map<Long, String> namesByLanguageId, NamePreference preference) {
        String mainValue = namesByLanguageId.get(preference.mainLanguageId());
        String subValue = preference.subLanguageId() == null
            ? null
            : namesByLanguageId.get(preference.subLanguageId());

        if (mainValue != null) {
            boolean bothPresentAndDifferent = subValue != null && !mainValue.equals(subValue);
            return bothPresentAndDifferent
                ? new ResolvedName(mainValue, subValue)
                : new ResolvedName(mainValue, null);
        }

        if (subValue != null) {
            return new ResolvedName(subValue, null);
        }

        for (Language language : languageRepository.findAllByOrderByDisplayOrderAsc()) {
            String value = namesByLanguageId.get(language.getId());
            if (value != null) {
                return new ResolvedName(value, null);
            }
        }

        return new ResolvedName(null, null);
    }
}
