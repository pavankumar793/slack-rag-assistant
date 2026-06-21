package com.pbot.backend.rag;

import com.pbot.backend.config.RagProperties;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class HttpDocumentReranker implements DocumentReranker {

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpDocumentReranker.class);

    private final RagProperties properties;
    private final RestClient restClient;
    private final DocumentReranker fallback = new NoOpDocumentReranker();

    public HttpDocumentReranker(RagProperties properties, RestClient.Builder restClientBuilder) {
        this.properties = properties;
        this.restClient = restClientBuilder.build();
    }

    @Override
    public List<Document> rerank(String query, List<Document> documents, int limit) {
        if (!properties.rerankerEnabled() || documents.isEmpty()) {
            return fallback.rerank(query, documents, limit);
        }

        try {
            RerankResponseItem[] response = restClient.post()
                    .uri(properties.rerankerUrl())
                    .body(new RerankRequest(query, documents.stream().map(Document::getText).toList(), false))
                    .retrieve()
                    .body(RerankResponseItem[].class);

            if (response == null || response.length == 0) {
                return fallback.rerank(query, documents, limit);
            }

            Map<Integer, Double> scoresByIndex = List.of(response).stream()
                    .filter(item -> item.index() != null && item.score() != null)
                    .collect(Collectors.toMap(RerankResponseItem::index, RerankResponseItem::score, Math::max));

            return IntStream.range(0, documents.size())
                    .mapToObj(index -> new IndexedDocument(index, documents.get(index)))
                    .filter(indexed -> scoresByIndex.containsKey(indexed.index()))
                    .sorted(Comparator.comparingDouble((IndexedDocument indexed) -> scoresByIndex.get(indexed.index()))
                            .reversed())
                    .limit(limit)
                    .map(IndexedDocument::document)
                    .toList();
        }
        catch (RuntimeException ex) {
            LOGGER.warn("Reranker request failed, falling back to similarity order: {}", ex.getMessage());
            return fallback.rerank(query, documents, limit);
        }
    }

    private record RerankRequest(String query, List<String> texts, boolean raw_scores) {
    }

    private record RerankResponseItem(Integer index, Double score) {
    }

    private record IndexedDocument(int index, Document document) {
    }
}
