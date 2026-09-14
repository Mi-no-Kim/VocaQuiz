package com.vocaquiz.admin.api.dto;

import jakarta.validation.constraints.NotBlank;

public record ExcludeVideoRequest(@NotBlank String reason) {
}
