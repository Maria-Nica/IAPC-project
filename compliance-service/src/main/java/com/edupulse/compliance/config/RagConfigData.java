package com.edupulse.compliance.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Retrieval settings bound from the "app.rag" section of application.yaml.
 *
 * @param topK                maximum number of chunks returned by the vector search
 * @param similarityThreshold minimum similarity score (0.0 - 1.0) a chunk must reach to be used
 */
@ConfigurationProperties(prefix = "app.rag")
public record RagConfigData(int topK, double similarityThreshold) {
}
