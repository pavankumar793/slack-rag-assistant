package com.pbot.backend.rag;

import com.pbot.backend.api.AskRequest;
import com.pbot.backend.api.AskResponse;
import com.pbot.backend.api.SourceDocument;
import com.pbot.backend.config.RagProperties;
import com.pbot.backend.llm.GitHubModelsClient;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

@Service
public class RagService {

    private final VectorStore vectorStore;
    private final GitHubModelsClient modelsClient;
    private final DocumentReranker reranker;
    private final ConversationMemoryService memoryService;
    private final RagProperties properties;

    public RagService(VectorStore vectorStore, GitHubModelsClient modelsClient,
            DocumentReranker reranker, ConversationMemoryService memoryService, RagProperties properties) {
        this.vectorStore = vectorStore;
        this.modelsClient = modelsClient;
        this.reranker = reranker;
        this.memoryService = memoryService;
        this.properties = properties;
    }

    public AskResponse ask(AskRequest request) {
        String conversationId = memoryService.conversationId(request.channelId(), request.threadTs(), request.userId());
        String memory = formatMemory(memoryService.recentTurns(conversationId));
        List<Document> matches = vectorStore.similaritySearch(SearchRequest.builder()
                .query(request.question())
                .topK(properties.topK())
                .similarityThreshold(properties.similarityThreshold())
                .build());

        List<Document> contextDocuments = reranker.rerank(request.question(), matches, properties.rerankedK());
        String context = contextDocuments.stream()
                .map(this::formatContextDocument)
                .collect(Collectors.joining("\n\n---\n\n"));

        String answer = modelsClient.complete("""
                        You are P-Bot, a concise Slack assistant.
                        Answer only from the provided context.
                        If the context does not contain the answer, say that you do not know.
                        Use conversation history only to understand follow-up questions.
                        Do not use conversation history as a source of factual answers.
                        Preserve facts from Markdown tables, including row labels, column labels, and cell values.
                        Include short source names when useful.
                        """, """
                        Conversation history:
                        %s

                        Question:
                        %s

                        Context:
                        %s
                        """.formatted(memory, request.question(),
                        context.isBlank() ? "No matching documents were found." : context));

        memoryService.remember(conversationId, request.question(), answer);
        return new AskResponse(answer, contextDocuments.stream().map(this::toSource).toList());
    }

    private String formatMemory(List<ConversationMemoryService.ConversationTurn> turns) {
        if (turns.isEmpty()) {
            return "No previous conversation.";
        }

        return turns.stream()
                .map(turn -> "User: " + turn.question() + "\nP-Bot: " + turn.answer())
                .collect(Collectors.joining("\n\n"));
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
