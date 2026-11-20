# EduPulse AI – Web Security Compliance Assistant

Multi-tenant compliance & audit assistant built on an Advanced RAG pipeline, focused on
web application security. It ingests official OWASP guidance and lets users ask questions
in natural language, check internal security policies against OWASP standards (gap analysis)
and get answers with exact source citations.

**Current scope: Epic 1 – RAG (deadline December 7).** Multi-tenancy and gap analysis are part
of the full project vision and come in later epics.

## Document topic: OWASP web application security

Knowledge base built only from credible, official sources:

- **OWASP Top 10** (2021, 2025) – the most critical web application security risks
- **OWASP ASVS** (Application Security Verification Standard) – detailed security requirements
- **OWASP Cheat Sheet Series** – practical prevention guidance
- **MITRE CWE** – common weakness definitions referenced by OWASP

New editions of these documents are ingested as new versions, without duplicating existing content.

Example questions:
- "How do I prevent SQL injection in a Spring Boot application?"
- "What changed in Broken Access Control between Top 10 2021 and 2025?"
- "Which ASVS requirements does our internal password policy not cover?"

## Tech stack

- Java 21, Spring Boot 4.0.7, Spring Cloud 2025.1.2
- Spring AI 2.0.0 (ChatClient, VectorStore, RetrievalAugmentationAdvisor)
- Qdrant 1.17 – vector store, accessed via gRPC (used instead of pgvector, as agreed in the lab)
- PostgreSQL 16 – relational data (planned: ingestion manifest, document versions)
- OpenAI – gpt-4o-mini (chat), text-embedding-3-small (embeddings)
- Docker & Docker Compose, Maven multi-module build

## Architecture

One microservice per story, following the course template:

| Module           | Story                          | Status      |
|------------------|--------------------------------|-------------|
| `ingest-static`  | 1 – Static ingestion           | planned     |
| `ingest-dynamic` | 2 – Dynamic ingestion          | planned     |
| `retrieval-pre`  | 3 – Pre-retrieval pipeline     | in progress |
| `retrieval-post` | 4 – Post-retrieval & grounding | skeleton    |
| `eureka-server`  | Infrastructure (discovery)     | planned     |
| `api-gateway`    | Infrastructure (port 8080)     | planned     |

Only the gateway will be public. Qdrant, PostgreSQL and the services are bound to localhost
or reachable only inside the Docker network. The shared chunk metadata contract is in
[docs/metadata-schema.md](docs/metadata-schema.md).

## Getting started

1. Clone the repository.
2. Copy `.env.example` to `.env` and fill in `LLM_API_KEY`, `POSTGRES_PASSWORD` and `QDRANT_API_KEY`
   (never commit `.env`). Compose refuses to start if a required value is empty.
3. Start everything:
   ```bash
   docker compose up --build -d
   docker compose ps
   ```
4. **Temporary step, until `ingest-static` exists** (it will be the only service that creates the
   Qdrant collection): create the empty collection once, using the `QDRANT_API_KEY` from your `.env`:
   ```bash
   curl -X PUT -H "api-key: <QDRANT_API_KEY>" -H "Content-Type: application/json" http://localhost:6333/collections/edupulse_rag -d '{"vectors":{"size":1536,"distance":"Cosine"}}'
   ```
5. Ask a question (temporary direct port, localhost only, until the gateway exists):
   ```bash
   curl -X POST -H "Content-Type: text/plain" --data "How do I prevent SQL injection?" http://localhost:8081/api/compliance/ask
   ```
   With no documents ingested yet, the expected answer is "I don't know".

## API – retrieval-pre

`POST /api/compliance/ask` – body: the question (plain text); optional `filter` URL parameter.

- Allowed fields: `doc_id`, `source`, `version`, `is_latest`, `page`
- Operators: `==`, `!=`, `in`, `>`, `>=`, `<`, `<=`, combined with `&&` and `||`
- Example: `?filter=source == 'owasp-top10-2021.pdf' && page > 3`
- An invalid filter returns `400` with the reason and the list of valid fields.

## Testing

- Unit and HTTP tests: `./mvnw test`
- Postman collection: [postman/retrieval-pre.postman_collection.json](postman/retrieval-pre.postman_collection.json)


## Team

| Member               | Epic 1 (RAG) responsibilities                            |
|----------------------|----------------------------------------------------------|
| Rautoiu Marco Andrei | Story 1 & 2 (tasks 1, 2) – static and dynamic ingestion  |
| Nica Maria Cristina  | Story 3 & 4 (tasks 1, 2) – pre- and post-retrieval       |

## Progress log

| Date       | Done | Issues / decisions |
|------------|------|--------------------|
| 2026-11-16 | Repository setup, README, Docker Compose with Qdrant + PostgreSQL, first RAG service with an `/ask` endpoint and source citations. | Topic chosen: OWASP. Qdrant chosen instead of pgvector (gRPC). |
| 2026-11-20 | Restructured to the course template: parent POM, Spring Boot 4 / Spring AI 2, `retrieval-pre` and `retrieval-post` modules, shared Dockerfile, compose and `.env` aligned (localhost-only ports). Shared metadata schema. **S3-T1 safe metadata filtering done** (whitelist, 400 with valid fields, AND/OR, 20 tests, Postman collection). | Qdrant upgraded to 1.17 to match the Spring AI 2.0 client. The OpenAI base URL needs the `/v1` suffix in Spring AI 2.0. Spring AI's Qdrant filters don't support booleans, so `is_latest` is stored as a string. Web sources use a logical `doc_id`, so new OWASP editions become versions of one document. |
