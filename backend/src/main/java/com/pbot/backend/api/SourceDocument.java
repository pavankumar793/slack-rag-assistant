package com.pbot.backend.api;

import java.util.Map;

public record SourceDocument(String id, String source, double score, Map<String, Object> metadata) {
}
