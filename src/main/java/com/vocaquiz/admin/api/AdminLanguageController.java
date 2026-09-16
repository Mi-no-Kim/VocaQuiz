package com.vocaquiz.admin.api;

import com.vocaquiz.admin.api.dto.AdminLanguageResponse;
import com.vocaquiz.catalog.service.LanguageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 언어 참조 목록 (D-036).
 *
 * <p>리포지토리를 직접 주입받지 않는다 (D-070) — 이 목록만 보여주는 화면이라도 예외가 없다.
 */
@RestController
@RequestMapping("/api/v1/admin/languages")
@RequiredArgsConstructor
public class AdminLanguageController {

    private final LanguageService languageService;

    @GetMapping
    public List<AdminLanguageResponse> list() {
        return languageService.list().stream()
            .map(AdminLanguageResponse::from)
            .toList();
    }
}
