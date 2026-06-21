package com.pbot.backend.rag;

import java.util.List;
import org.springframework.ai.document.Document;

public class NoOpDocumentReranker implements DocumentReranker {

    @Override
    public List<Document> rerank(String query, List<Document> documents, int limit) {
        return documents.stream().limit(limit).toList();
    }
}
