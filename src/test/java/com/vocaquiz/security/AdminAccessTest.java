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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * D-032 · /admin/** 은 role=ADMIN 만.
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
    @DisplayName("익명은 로그인으로 리다이렉트된다")
    void anonymousIsRedirected() throws Exception {
        mockMvc.perform(get("/api/v1/admin/me"))
            .andExpect(status().is3xxRedirection());
    }
}
