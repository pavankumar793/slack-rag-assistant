# slack-rag-assistant

P-Bot is a RAG Slack assistant with two parts:

- `backend`: Spring Boot + Spring AI + Qdrant + local ONNX/Transformers embeddings + GitHub Models chat.
- `slack-bot`: Node.js Slack Bolt app that listens for mentions and calls the backend.

## Local Setup

Create a GitHub fine-grained personal access token with `models:read`, then set
it locally:

```powershell
$env:GITHUB_PERSONAL_ACCESS_TOKEN="your-token"
```

Start Qdrant and the local BGE reranker with Podman:

```powershell
podman compose up -d qdrant reranker
```

Run the backend:

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

The backend resets the Qdrant collection and ingests local docs from `docs/` on
startup. To refresh the vector database after changing docs without restarting
the app, call:

```powershell
curl -X POST http://localhost:8080/api/ingest
```

Ask directly:

```powershell
curl -X POST http://localhost:8080/api/ask ^
  -H "Content-Type: application/json" ^
  -d "{\"question\":\"What is P-Bot?\"}"
```

Run the Slack bot:

```powershell
cd slack-bot
copy .env.example .env
npm install
npm run dev
```

## Configuration

Backend environment variables:

- `GITHUB_PERSONAL_ACCESS_TOKEN`: GitHub token with `models:read`, required for answer generation.
- `PBOT_GITHUB_MODEL`: GitHub Models model ID, default `openai/gpt-4.1-mini`.
- `PBOT_GITHUB_MODELS_ENDPOINT`: default `https://models.github.ai/inference/chat/completions`.
- `PBOT_LLM_MAX_TOKENS`: max generated tokens, default `1200`.
- `PBOT_LLM_TEMPERATURE`: default `0.2`.
- `PBOT_COMPOSE_ENABLED`: Spring Boot Docker Compose integration, default `false`.
- `QDRANT_HOST`: default `localhost`.
- `QDRANT_GRPC_PORT`: default `6334`.
- `QDRANT_COLLECTION`: default `pbot_docs`.
- `PBOT_DOCS_PATH`: default `../docs`.
- `PBOT_RAG_TOP_K`: Qdrant candidate chunks to retrieve before reranking, default `50`.
- `PBOT_RAG_RERANKED_K`: reranked chunks sent to the LLM, default `8`.
- `PBOT_INGEST_ON_STARTUP`: default `true`.
- `PBOT_MEMORY_ENABLED`: include recent Slack thread history for follow-up questions, default `true`.
- `PBOT_MEMORY_MAX_TURNS`: number of recent user/bot turns to keep per conversation, default `6`.
- `PBOT_RERANKER_ENABLED`: default `true`.
- `PBOT_RERANKER_URL`: default `http://localhost:8081/rerank`.

GitHub Models free API usage is rate limited. If the free limit is reached and
paid usage is not enabled for your account or organization, requests should fail
with a GitHub API error instead of falling back to paid local behavior.

Slack bot environment variables are listed in `slack-bot/.env.example`.
Set `PBOT_REPLY_IN_THREAD=false` in `slack-bot/.env` if you want final answers
posted in the channel instead of a thread.

## Current RAG Flow

```text
Slack mention
  -> Node Slack bot
  -> POST /api/ask
  -> thread-scoped in-memory conversation history
  -> Qdrant similarity search candidate top 50
  -> local BGE reranker selects best 8 chunks
  -> GitHub Models answer with sources
  -> Slack thread update
```

If the reranker service is unavailable, the backend logs a warning and falls
back to Qdrant similarity order.
