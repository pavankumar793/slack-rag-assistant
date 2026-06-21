# P-Bot

P-Bot is a Slack assistant backed by a Spring AI retrieval augmented generation service.

When a Slack user mentions P-Bot, the Slack bot sends the question to the backend.
The backend retrieves relevant documentation chunks from Qdrant, reranks the matches,
and asks the configured LLM to answer using only the retrieved context.

For local development, add Markdown or text documents to this folder and call
`POST /api/ingest` on the backend.
