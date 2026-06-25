package com.pbot.backend.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "pbot.llm.github")
public record GitHubModelsProperties(
        @NotBlank String endpoint,
        String token,
        @NotBlank String model,
        @NotBlank String apiVersion,
        double temperature,
        @Positive int maxTokens) {
}
