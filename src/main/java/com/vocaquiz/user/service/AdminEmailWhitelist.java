package com.vocaquiz.user.service;

import com.vocaquiz.user.domain.Role;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

@Component
@Slf4j
public class AdminEmailWhitelist {

    private final Set<String> adminEmailSet;

    public AdminEmailWhitelist(@Value("${app.admin-emails:}") String emails) {
        adminEmailSet = Collections.unmodifiableSet(parseEmails(emails));
        log.info("관리자 화이트리스트 {}개 등록", adminEmailSet.size());
    }

    private Set<String> parseEmails(String emails) {
        Set<String> set = new HashSet<>();

        if (emails == null || emails.isEmpty()) return set;

        for (String target : emails.trim().split(",")) {
            String email = target.trim().toLowerCase(Locale.ROOT);
            if (email.isEmpty()) continue;
            set.add(email);
        }
        return set;
    }

    public Role roleFor(String email) {
        if (email == null)
            return Role.USER;
        if (adminEmailSet.contains(email.trim().toLowerCase(Locale.ROOT)))
            return Role.ADMIN;
        else
            return Role.USER;
    }
}
