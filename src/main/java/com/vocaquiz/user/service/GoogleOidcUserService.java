package com.vocaquiz.user.service;

import com.vocaquiz.user.domain.AppUser;
import com.vocaquiz.user.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.vocaquiz.user.domain.AuthProvider.GOOGLE;

@Service
@Transactional
@RequiredArgsConstructor
public class GoogleOidcUserService extends OidcUserService {

    private final AppUserRepository appUserRepository;
    private final AdminEmailWhitelist adminEmailWhitelist;

    private static final String SUBJECT_ATTRIBUTE = "sub";

    @Override
    public OidcUser loadUser(OidcUserRequest request) {
        OidcUser oidcUser = super.loadUser(request);

        String providerUserId = oidcUser.getSubject();
        String email = oidcUser.getEmail();

        AppUser user = appUserRepository.findByProviderAndProviderUserId(GOOGLE, providerUserId)
            .orElseGet(() -> appUserRepository.save(
                AppUser.create(GOOGLE, providerUserId, email, adminEmailWhitelist.roleFor(email))));


        return new DefaultOidcUser(
            List.of(new SimpleGrantedAuthority(user.getRole().authority())),
            oidcUser.getIdToken(),
            oidcUser.getUserInfo(),
            SUBJECT_ATTRIBUTE
        );
    }
}
