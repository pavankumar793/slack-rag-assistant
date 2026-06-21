package com.pbot.backend.api;

public record IngestResponse(int loadedDocuments, int storedChunks) {
}
