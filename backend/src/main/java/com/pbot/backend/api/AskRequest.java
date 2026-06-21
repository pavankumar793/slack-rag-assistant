package com.pbot.backend.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AskRequest(
        @NotBlank @Size(max = 4000) String question,
        String userId,
        String channelId,
        String threadTs) {
}
