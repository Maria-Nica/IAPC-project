-- EduPulse AI - database structure
-- Runs automatically only once, when the postgres-data volume is created for the first time.
-- To re-run it locally: docker compose down -v && docker compose up -d (this deletes local data).

-- Column-level encryption for sensitive data (NFR: data-at-rest encryption, AES-256)
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- One schema per microservice, so each service owns its own tables
CREATE SCHEMA IF NOT EXISTS compliance;
CREATE SCHEMA IF NOT EXISTS ingestion;
