package com.pbot.backend.rag;

import com.pbot.backend.config.RagProperties;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ConversationMemoryService {

    private final RagProperties properties;
    private final Map<String, Deque<ConversationTurn>> conversations = new ConcurrentHashMap<>();

    public ConversationMemoryService(RagProperties properties) {
        this.properties = properties;
    }

    public String conversationId(String channelId, String threadTs, String userId) {
        if (StringUtils.hasText(channelId) && StringUtils.hasText(threadTs)) {
            return channelId + ":" + threadTs;
        }
        if (StringUtils.hasText(channelId) && StringUtils.hasText(userId)) {
            return channelId + ":" + userId;
        }
        if (StringUtils.hasText(userId)) {
            return userId;
        }
        return "default";
    }

    public List<ConversationTurn> recentTurns(String conversationId) {
        if (!properties.memoryEnabled()) {
            return List.of();
        }
        return List.copyOf(conversations.getOrDefault(conversationId, new ArrayDeque<>()));
    }

    public void remember(String conversationId, String question, String answer) {
        if (!properties.memoryEnabled()) {
            return;
        }

        Deque<ConversationTurn> turns = conversations.computeIfAbsent(conversationId, ignored -> new ArrayDeque<>());
        synchronized (turns) {
            turns.addLast(new ConversationTurn(question, answer));
            while (turns.size() > properties.memoryMaxTurns()) {
                turns.removeFirst();
            }
        }
    }

    public record ConversationTurn(String question, String answer) {
    }
}
