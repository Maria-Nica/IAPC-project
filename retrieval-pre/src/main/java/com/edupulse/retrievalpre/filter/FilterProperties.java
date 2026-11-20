package com.edupulse.retrievalpre.filter;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Whitelist for metadata filters received as URL parameters, bound from "app.retrieval.filter".
 *
 * @param allowedFields    metadata fields a filter may use (see docs/metadata-schema.md)
 * @param allowedOperators comparison operators a filter may use (eq, ne, in, gt, gte, lt, lte)
 */
@ConfigurationProperties(prefix = "app.retrieval.filter")
public record FilterProperties(List<String> allowedFields, List<String> allowedOperators) {

    public FilterProperties {
        allowedFields = allowedFields == null ? List.of() : List.copyOf(allowedFields);
        allowedOperators = allowedOperators == null ? List.of() : List.copyOf(allowedOperators);
    }
}
