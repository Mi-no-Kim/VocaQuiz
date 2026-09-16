package com.vocaquiz.admin.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** {@code id}가 없으면 새 이름, 있으면 그 행을 고친다 (B2). */
public record UpdateSongNameRequest(
    Long id,
    @NotNull Long languageId,
    @NotBlank String name,
    boolean primary
) {
}
