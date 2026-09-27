package com.vocaquiz.user.service;

import com.vocaquiz.catalog.repository.LanguageRepository;
import com.vocaquiz.catalog.service.NamePreference;
import com.vocaquiz.user.domain.AppUser;
import com.vocaquiz.user.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static com.vocaquiz.user.domain.AuthProvider.GOOGLE;

/**
 * 로그인한 관리자의 이름 표시 설정(곡명·작곡가명 main/sub, D-074)을 읽는다.
 *
 * <p>{@code @WithMockUser}처럼 실제 구글 로그인이 아닌 principal이거나 그에 대응하는
 * {@link AppUser} 행이 없으면 기본값(EN, sub 없음)으로 돌아간다. 이 화면들은 전부 ADMIN
 * 권한이 있어야 닿고, 실제 로그인은 항상 {@link AppUser} 행을 만들어 두므로
 * ({@link GoogleOidcUserService}) 운영에서는 이 기본값 경로를 타지 않는다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DisplayPreferenceService {

    private final AppUserRepository appUserRepository;
    private final LanguageRepository languageRepository;

    public NamePreference songNamePreference(Authentication authentication) {
        return currentUser(authentication)
            .map(u -> toPreference(u.getSongMainLanguage().getId(), u.getSongSubLanguage()))
            .orElseGet(this::defaultPreference);
    }

    public NamePreference producerNamePreference(Authentication authentication) {
        return currentUser(authentication)
            .map(u -> toPreference(u.getProducerMainLanguage().getId(), u.getProducerSubLanguage()))
            .orElseGet(this::defaultPreference);
    }

    private NamePreference toPreference(Long mainLanguageId, com.vocaquiz.catalog.domain.Language sub) {
        return new NamePreference(mainLanguageId, sub == null ? null : sub.getId());
    }

    private Optional<AppUser> currentUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof OidcUser oidcUser)) {
            return Optional.empty();
        }
        return appUserRepository.findByProviderAndProviderUserId(GOOGLE, oidcUser.getSubject());
    }

    private NamePreference defaultPreference() {
        Long enId = languageRepository.findByCode("EN")
            .orElseThrow(() -> new IllegalStateException("EN language is not seeded"))
            .getId();
        return new NamePreference(enId, null);
    }
}
