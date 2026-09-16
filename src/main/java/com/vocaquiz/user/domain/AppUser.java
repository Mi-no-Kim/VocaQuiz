package com.vocaquiz.user.domain;

import com.vocaquiz.catalog.domain.Language;
import com.vocaquiz.common.domain.CreatedAtEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "app_user",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_app_user_provider",
        columnNames = {"provider", "provider_user_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AppUser extends CreatedAtEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 20, nullable = false)
    @Enumerated(value = EnumType.STRING)
    private AuthProvider provider;

    @Column(length = 100, nullable = false)
    private String providerUserId;

    @Column(length = 200)
    private String email;

    @Column(length = 20, nullable = false)
    @Enumerated(value = EnumType.STRING)
    private Role role;

    /** 표시 언어 — 곡명·프로듀서명 등 이름 표시에 쓴다. 첫 로그인에 English로 채워진다 (D-071). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_language_id", nullable = false)
    private Language siteLanguage;

    public static AppUser create(
            AuthProvider provider, String providerUserId, String email, Role role, Language siteLanguage) {
        AppUser appUser = new AppUser();
        appUser.provider = provider;
        appUser.providerUserId = providerUserId;
        appUser.email = email;
        appUser.role = role;
        appUser.siteLanguage = siteLanguage;

        return appUser;
    }

    /** 사이트 언어 설정을 바꾼다 (D-071). */
    public void changeSiteLanguage(Language siteLanguage) {
        this.siteLanguage = siteLanguage;
    }
}
