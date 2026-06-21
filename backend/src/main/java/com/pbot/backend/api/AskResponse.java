package com.pbot.backend.api;

import java.util.List;

public record AskResponse(String answer, List<SourceDocument> sources) {
}
