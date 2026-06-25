package com.pbot.backend.llm;

import com.pbot.backend.config.GitHubModelsProperties;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class GitHubModelsClient {

    private final GitHubModelsProperties properties;
    private final RestClient restClient;

    public GitHubModelsClient(GitHubModelsProperties properties, RestClient.Builder restClientBuilder) {
        this.properties = properties;
        this.restClient = restClientBuilder.build();
    }

    public String complete(String systemPrompt, String userPrompt) {
        if (!StringUtils.hasText(properties.token())) {
            throw new IllegalStateException("GITHUB_TOKEN is required for GitHub Models inference.");
        }

        try {
            ChatCompletionResponse response = restClient.post()
                    .uri(properties.endpoint())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.token())
                    .header(HttpHeaders.ACCEPT, "application/vnd.github+json")
                    .header("X-GitHub-Api-Version", properties.apiVersion())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new ChatCompletionRequest(
                            properties.model(),
                            List.of(new Message("system", systemPrompt), new Message("user", userPrompt)),
                            properties.temperature(),
                            properties.maxTokens(),
                            false))
                    .retrieve()
                    .body(ChatCompletionResponse.class);

            if (response == null || response.choices() == null || response.choices().isEmpty()) {
                throw new IllegalStateException("GitHub Models returned no choices.");
            }

            Message message = response.choices().getFirst().message();
            if (message == null || !StringUtils.hasText(message.content())) {
                throw new IllegalStateException("GitHub Models returned an empty response.");
            }

            return message.content();
        }
        catch (RestClientResponseException ex) {
            throw new IllegalStateException("GitHub Models request failed with HTTP " + ex.getStatusCode()
                    + ": " + ex.getResponseBodyAsString(), ex);
        }
    }

    private record ChatCompletionRequest(
            String model,
            List<Message> messages,
            double temperature,
            int max_tokens,
            boolean stream) {
    }

    private record ChatCompletionResponse(List<Choice> choices) {
    }

    private record Choice(Message message) {
    }

    private record Message(String role, String content) {
    }
}
