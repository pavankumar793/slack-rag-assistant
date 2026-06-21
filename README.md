# P-Bot

P-Bot is a RAG Slack assistant with two parts:

- `backend`: Spring Boot + Spring AI + Qdrant + local ONNX/Transformers embeddings + Ollama chat.
- `slack-bot`: Node.js Slack Bolt app that listens for mentions and calls the backend.

## Local Setup

Pull the local Ollama chat model once:

```bash
ollama pull qwen2.5:7b
```

Start Qdrant and the local BGE reranker:

```bash
docker compose up -d qdrant reranker
```

Run the backend:

```bash
cd backend
.\mvnw.cmd spring-boot:run
```

The backend ingests local docs from `docs/` on startup. To refresh the vector
database after changing docs without restarting the app, call:

```bash
curl -X POST http://localhost:8080/api/ingest
```

Ask directly:

```bash
curl -X POST http://localhost:8080/api/ask ^
  -H "Content-Type: application/json" ^
  -d "{\"question\":\"What is P-Bot?\"}"
```

Run the Slack bot:

```bash
cd slack-bot
copy .env.example .env
npm install
npm run dev
```

## Configuration

Backend environment variables:

- `OLLAMA_BASE_URL`: Ollama endpoint, default `http://localhost:11434`.
- `PBOT_OLLAMA_CHAT_MODEL`: chat model, default `qwen2.5:7b`.
- `QDRANT_HOST`: default `localhost`.
- `QDRANT_GRPC_PORT`: default `6334`.
- `QDRANT_COLLECTION`: default `pbot_docs`.
- `PBOT_DOCS_PATH`: default `../docs`.
- `PBOT_RAG_TOP_K`: Qdrant candidate chunks to retrieve before reranking, default `50`.
- `PBOT_RAG_RERANKED_K`: reranked chunks sent to Ollama, default `8`.
- `PBOT_INGEST_ON_STARTUP`: default `true`.
- `PBOT_RERANKER_ENABLED`: default `true`.
- `PBOT_RERANKER_URL`: default `http://localhost:8081/rerank`.

Slack bot environment variables are listed in `slack-bot/.env.example`.
Set `PBOT_REPLY_IN_THREAD=false` in `slack-bot/.env` if you want final answers
posted in the channel instead of a thread.

## Current RAG Flow

```text
Slack mention
  -> Node Slack bot
  -> POST /api/ask
  -> Qdrant similarity search candidate top 50
  -> local BGE reranker selects best 8 chunks
  -> LLM answer with sources
  -> Slack thread update
```

If the reranker service is unavailable, the backend logs a warning and falls
back to Qdrant similarity order.
