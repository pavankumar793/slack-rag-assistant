package com.pbot.backend.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "pbot.rag")
public record RagProperties(
        @NotBlank String documentsPath,
        @Min(1) @Max(50) int topK,
        @Min(1) @Max(20) int rerankedK,
        double similarityThreshold,
        boolean ingestOnStartup,
        boolean rerankerEnabled,
        @NotBlank String rerankerUrl) {
}
