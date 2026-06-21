package com.pbot.backend.rag;

import com.pbot.backend.api.AskRequest;
import com.pbot.backend.api.AskResponse;
import com.pbot.backend.api.SourceDocument;
import com.pbot.backend.config.RagProperties;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

@Service
public class RagService {

    private final VectorStore vectorStore;
    private final ChatClient chatClient;
    private final DocumentReranker reranker;
    private final RagProperties properties;

    public RagService(VectorStore vectorStore, ChatClient.Builder chatClientBuilder,
            DocumentReranker reranker, RagProperties properties) {
        this.vectorStore = vectorStore;
        this.chatClient = chatClientBuilder.build();
        this.reranker = reranker;
        this.properties = properties;
    }

    public AskResponse ask(AskRequest request) {
        List<Document> matches = vectorStore.similaritySearch(SearchRequest.builder()
                .query(request.question())
                .topK(properties.topK())
                .similarityThreshold(properties.similarityThreshold())
                .build());

        List<Document> contextDocuments = reranker.rerank(request.question(), matches, properties.rerankedK());
        String context = contextDocuments.stream()
                .map(this::formatContextDocument)
                .collect(Collectors.joining("\n\n---\n\n"));

        String answer = chatClient.prompt()
                .system("""
                        You are P-Bot, a concise Slack assistant.
                        Answer only from the provided context.
                        If the context does not contain the answer, say that you do not know.
                        Include short source names when useful.
                        """)
                .user(user -> user.text("""
                        Question:
                        {question}

                        Context:
                        {context}
                        """)
                        .param("question", request.question())
                        .param("context", context.isBlank() ? "No matching documents were found." : context))
                .call()
                .content();

        return new AskResponse(answer, contextDocuments.stream().map(this::toSource).toList());
    }

    private String formatContextDocument(Document document) {
        String source = String.valueOf(document.getMetadata().getOrDefault("source", "unknown"));
        return "Source: " + source + "\n" + document.getText();
    }

    private SourceDocument toSource(Document document) {
        Map<String, Object> metadata = document.getMetadata();
        String id = document.getId();
        String source = String.valueOf(metadata.getOrDefault("source", "unknown"));
        double score = document.getScore() == null ? 0.0 : document.getScore();
        return new SourceDocument(id, source, score, metadata);
    }
}
