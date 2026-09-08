package com.vocaquiz.common.config;

import com.vocaquiz.user.service.GoogleOidcUserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /** /admin/** 은 role=ADMIN 만 (D-032). 나머지는 익명 허용 — 게임 플레이 때문이다. */
    private static final String[] ADMIN_PATHS = {"/admin", "/admin/**", "/api/v1/admin/**"};

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, GoogleOidcUserService userService) throws Exception {
        return http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(ADMIN_PATHS).hasRole("ADMIN")
                .anyRequest().permitAll())
            .oauth2Login(oauth2 -> oauth2
                .userInfoEndpoint(userInfo -> userInfo.oidcUserService(userService)))
            .build();
    }
}
