# Chunk metadata schema

Shared contract between the ingestion services (`ingest-static`, `ingest-dynamic`) that **write**
chunks to Qdrant and the retrieval services (`retrieval-pre`, `retrieval-post`) that **read** them.
Every chunk stored in the `edupulse_rag` collection must carry these fields with exactly these
names and types. Changing a field name or type requires updating both sides.

## Fields

| Field         | Type                  | Required         | Description                                                                                   | Used by                                    |
|---------------|-----------------------|------------------|-----------------------------------------------------------------------------------------------|--------------------------------------------|
| `doc_id`      | string                | always           | Stable document identifier, the **same for all versions** of a document (see rules below)     | update/delete, neighbor stitching, filters |
| `version`     | integer (1, 2, 3 ...) | always           | Document version; the first ingestion is `1`                                                  | versioning, citations, filters             |
| `source`      | string                | always           | File name (e.g. `owasp-top10-2021.pdf`) or page title for web sources                         | citations, filters                         |
| `page`        | integer               | always           | Page number in the original document (`1` for sources without pages). Spring AI's PDF readers write this as `page_number`, so it must be renamed to `page` during ingestion | citations, filters                         |
| `chunk_index` | integer, from `0`     | always           | Position of the chunk inside one version of the document                                      | neighbor stitching                         |
| `checksum`    | string (SHA-256, hex) | always           | Hash of the whole source file/content, used to detect changes                                 | update process                             |
| `is_latest`   | string `"true"`/`"false"` | always    | `"true"` only for chunks of the newest version. Stored as a **string**, because Spring AI's Qdrant filter converter does not support boolean values | default retrieval, filters                 |
| `valid_from`  | string (ISO-8601 UTC) | always           | When this version was ingested, e.g. `2026-11-20T14:30:00Z`                                   | versioning                                 |
| `source_url`  | string (URL)          | web sources only | Original URL of the page, remote PDF or repository                                            | citations, audit                           |
| `fetched_at`  | string (ISO-8601 UTC) | web sources only | When the content was downloaded                                                               | audit                                      |

## Rules

1. **Stable `doc_id`**: derived from the identity of the source, not from its content, so a new
   version keeps the same `doc_id`.
   - Static files: SHA-256 of the file path relative to the resources folder (e.g. `owasp/top10-2021.pdf`).
   - Web sources: SHA-256 of the normalized URL.
2. **Qdrant point ID**: a deterministic UUID built from `doc_id` + `version` + `chunk_index`
   (`UUID.nameUUIDFromBytes(...)`). Re-ingesting the same file overwrites the same points instead
   of creating duplicates.
3. **Updates and versioning** differ per service:
   - `ingest-static` (Story 1, Task 2): a changed file (new `checksum`) **replaces** the old chunks
     atomically. Queries never see a mix of old and new content, and a failure mid-update leaves
     the previous version intact. Only one version of a static file stays in the store.
   - `ingest-dynamic` (Story 2, Task 2): a new version **keeps** the old chunks, marked
     `is_latest = "false"` with their `valid_from`. Retrieval returns only `is_latest = "true"` unless
     a version is requested explicitly.
4. **Neighbor stitching** looks up adjacent chunks by `doc_id` **and** `version` and `chunk_index`,
   so it never crosses document boundaries or mixes two versions of the same document.
5. **Filterable fields** (whitelist used by `retrieval-pre`): `doc_id`, `source`, `version`,
   `is_latest`, `page`. Allowed operators: `eq`, `ne`, `in`, `gt`, `gte`, `lt`, `lte`.
6. **Citation header** built by the retrieval side from the metadata:
   `[source: <source>, version: <version>, page: <page>]`.

## Example (static PDF chunk)

```json
{
  "doc_id": "3f5a9c1e...",
  "version": 2,
  "source": "owasp-top10-2021.pdf",
  "page": 7,
  "chunk_index": 12,
  "checksum": "9b2d4e7f...",
  "is_latest": "true",
  "valid_from": "2026-11-24T10:15:00Z"
}
```

## Example (web chunk)

```json
{
  "doc_id": "a81c0d42...",
  "version": 1,
  "source": "SQL Injection Prevention Cheat Sheet",
  "page": 1,
  "chunk_index": 3,
  "checksum": "c4e1f0a9...",
  "is_latest": "true",
  "valid_from": "2026-11-30T09:00:00Z",
  "source_url": "https://cheatsheetseries.owasp.org/cheatsheets/SQL_Injection_Prevention_Cheat_Sheet.html",
  "fetched_at": "2026-11-30T09:00:00Z"
}
```
