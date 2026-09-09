package com.vocaquiz.youtube.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "youtube")
@Validated
public record YoutubeProperties(
    @NotBlank String apiKey,
    @NotBlank String baseUrl
) {
}
