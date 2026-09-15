package com.vocaquiz.admin.api;

import com.vocaquiz.admin.api.dto.AdminMeResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    @GetMapping("/me")
    public AdminMeResponse me(Authentication authentication) {
        List<String> authorities = authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .toList();

        // 실제 로그인(OidcUser)이면 이메일을, AdminAccessTest의 @WithMockUser처럼
        // OidcUser가 아닌 principal이면 authentication.getName()으로 대신한다.
        String email = authentication.getPrincipal() instanceof OidcUser oidcUser
            ? oidcUser.getEmail()
            : authentication.getName();

        return new AdminMeResponse(email, authorities);
    }
}
