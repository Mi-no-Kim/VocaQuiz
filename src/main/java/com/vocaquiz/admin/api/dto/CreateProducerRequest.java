package com.vocaquiz.admin.api.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateProducerRequest(
    @NotBlank String name
) {
}
