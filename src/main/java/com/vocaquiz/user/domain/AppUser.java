package com.vocaquiz.user.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(
    name = "app_user",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_app_user_provider",
        columnNames = {"provider", "provider_user_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AppUser {
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

    @Column(nullable = false)
    private Instant createdAt;

    public static AppUser create(AuthProvider provider, String providerUserId, String email, Role role) {
        AppUser appUser = new AppUser();
        appUser.provider = provider;
        appUser.providerUserId = providerUserId;
        appUser.email = email;
        appUser.role = role;
        appUser.createdAt = Instant.now();

        return appUser;
    }
}
