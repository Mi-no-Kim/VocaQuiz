package com.vocaquiz.user.service;

import com.vocaquiz.user.domain.AppUser;
import com.vocaquiz.user.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.vocaquiz.user.domain.AuthProvider.GOOGLE;

@Service
@Transactional
@RequiredArgsConstructor
public class GoogleOAuth2UserService extends DefaultOAuth2UserService {

    private final AppUserRepository appUserRepository;
    private final AdminEmailWhitelist adminEmailWhitelist;

    private static final String SUBJECT_ATTRIBUTE = "sub";

    @Override
    public OAuth2User loadUser(OAuth2UserRequest request) {
        OAuth2User oAuth2User = super.loadUser(request);

        String providerUserId = oAuth2User.getAttribute(SUBJECT_ATTRIBUTE);
        String email = oAuth2User.getAttribute("email");

        AppUser user = appUserRepository.findByProviderAndProviderUserId(GOOGLE, providerUserId)
            .orElseGet(() -> appUserRepository.save(
                AppUser.create(GOOGLE, providerUserId, email, adminEmailWhitelist.roleFor(email))));


        return new DefaultOAuth2User(
            List.of(new SimpleGrantedAuthority(user.getRole().authority())),
            oAuth2User.getAttributes(),
            SUBJECT_ATTRIBUTE
        );
    }
}
