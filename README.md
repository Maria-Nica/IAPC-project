# EduPulse AI – Web Security Compliance Assistant

Multi-tenant compliance & audit assistant built on an Advanced RAG pipeline, focused on
web application security. It ingests official OWASP guidance and lets users ask questions
in natural language, check internal security policies against OWASP standards (gap analysis)
and get answers with exact source citations.

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

- Java 21, Spring Boot 3.x
- Spring AI (ChatClient, VectorStore, advisors)
- PostgreSQL + pgvector (vector search) and tsvector (full-text search)
- OpenAI (chat + embeddings)
- Docker & Docker Compose
- Considered alternative: Qdrant (gRPC) as vector store
