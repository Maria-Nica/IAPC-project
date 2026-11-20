package com.edupulse.retrievalpre.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Retrieval settings bound from the "app.retrieval" section of application.yaml.
 *
 * @param topK                maximum number of chunks returned by the vector search
 * @param similarityThreshold minimum similarity score (0.0 - 1.0) a chunk must reach to be used
 */
@ConfigurationProperties(prefix = "app.retrieval")
public record RagConfigData(int topK, double similarityThreshold) {
}
