package com.vocaquiz.admin.api.dto;

import com.vocaquiz.catalog.service.LanguageSummary;

public record AdminLanguageResponse(
    Long id,
    String code,
    String name
) {

    public static AdminLanguageResponse from(LanguageSummary summary) {
        return new AdminLanguageResponse(summary.id(), summary.code(), summary.name());
    }
}
