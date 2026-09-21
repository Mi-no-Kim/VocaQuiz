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

    /** 사이트에서 쓸 언어(UI 언어) — 곡명·작곡가명 등 이름 표시는 D-074가 대신 맡는다. 첫 로그인에 English로 채워진다 (D-071, D-074). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_language_id", nullable = false)
    private Language siteLanguage;

    /**
     * 곡명·작곡가명 표시 설정 (D-074) — 각각 main(필수)·sub(선택) 두 언어로 정한다.
     * 표시 규칙은 {@code catalog.service.NameDisplayResolver}가 맡는다.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "song_main_language_id", nullable = false)
    private Language songMainLanguage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "song_sub_language_id")
    private Language songSubLanguage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producer_main_language_id", nullable = false)
    private Language producerMainLanguage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producer_sub_language_id")
    private Language producerSubLanguage;

    public static AppUser create(
            AuthProvider provider,
            String providerUserId,
            String email,
            Role role,
            Language siteLanguage,
            Language songMainLanguage,
            Language songSubLanguage,
            Language producerMainLanguage,
            Language producerSubLanguage) {
        requireDistinct(songMainLanguage, songSubLanguage);
        requireDistinct(producerMainLanguage, producerSubLanguage);

        AppUser appUser = new AppUser();
        appUser.provider = provider;
        appUser.providerUserId = providerUserId;
        appUser.email = email;
        appUser.role = role;
        appUser.siteLanguage = siteLanguage;
        appUser.songMainLanguage = songMainLanguage;
        appUser.songSubLanguage = songSubLanguage;
        appUser.producerMainLanguage = producerMainLanguage;
        appUser.producerSubLanguage = producerSubLanguage;

        return appUser;
    }

    /** 사이트 언어 설정을 바꾼다 (D-071). */
    public void changeSiteLanguage(Language siteLanguage) {
        this.siteLanguage = siteLanguage;
    }

    /** main·sub가 같은 언어면 안 된다 (D-074) — sub가 없으면(null) 검사하지 않는다. */
    private static void requireDistinct(Language main, Language sub) {
        if (sub != null && main.getId().equals(sub.getId())) {
            throw new IllegalArgumentException("main과 sub 표기법은 같은 언어일 수 없다");
        }
    }
}
