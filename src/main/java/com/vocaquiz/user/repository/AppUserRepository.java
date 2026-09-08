package com.vocaquiz.user.repository;

import com.vocaquiz.user.domain.AppUser;
import com.vocaquiz.user.domain.AuthProvider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByProviderAndProviderUserId(AuthProvider provider, String providerUserId);
}
