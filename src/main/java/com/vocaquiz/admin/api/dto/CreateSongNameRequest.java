package com.vocaquiz.admin.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateSongNameRequest(
    @NotNull Long languageId,
    @NotBlank String name,
    boolean primary
) {
}
