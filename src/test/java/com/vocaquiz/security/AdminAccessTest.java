package com.vocaquiz.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * D-032 · /admin/** 은 role=ADMIN 만.
 * D-065 · /api/** 는 로그인이 없으면 401, 상태를 바꾸는 요청에는 CSRF 토큰이 필요하다.
 * 실제 구글 로그인은 사람이 확인한다. 여기서는 권한 규칙만 검증한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class AdminAccessTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("ADMIN은 관리자 API에 접근한다")
    void adminCanAccess() throws Exception {
        mockMvc.perform(get("/api/v1/admin/me"))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("화이트리스트에 없는 계정은 403")
    void nonAdminIsForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/admin/me"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithAnonymousUser
    @DisplayName("익명이 관리자 API를 부르면 401 — 리다이렉트하지 않는다")
    void anonymousGetsUnauthorizedOnApi() throws Exception {
        mockMvc.perform(get("/api/v1/admin/me"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithAnonymousUser
    @DisplayName("익명이 화면 경로를 열면 로그인으로 리다이렉트된다")
    void anonymousIsRedirectedOnScreen() throws Exception {
        mockMvc.perform(get("/admin"))
            .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("CSRF 토큰 없는 POST는 403으로 막힌다")
    void postWithoutCsrfTokenIsRejected() throws Exception {
        mockMvc.perform(post("/api/v1/admin/me"))
            .andExpect(status().isForbidden());
    }

    /** 토큰이 있으면 CSRF 단계를 지나 디스패처까지 간다. /me는 GET 전용이라 405가 정상이다. */
    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("CSRF 토큰이 있으면 CSRF에서 막히지 않는다")
    void postWithCsrfTokenPassesCsrf() throws Exception {
        mockMvc.perform(post("/api/v1/admin/me").with(csrf()))
            .andExpect(status().isMethodNotAllowed());
    }
}
