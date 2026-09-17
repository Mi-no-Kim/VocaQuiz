package com.vocaquiz.admin.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateProducerNameRequest(
    @NotNull Long languageId,
    @NotBlank String name,
    boolean primary
) {
}
