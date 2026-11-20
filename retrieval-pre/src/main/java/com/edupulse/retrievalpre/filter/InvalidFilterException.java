package com.edupulse.retrievalpre.filter;

import java.util.List;

/**
 * Thrown when a metadata filter from the URL is malformed or uses a field, operator or value
 * that is not allowed. Carries the whitelist so the API can tell the client what is valid.
 */
public class InvalidFilterException extends RuntimeException {

    private final List<String> validFields;
    private final List<String> validOperators;

    public InvalidFilterException(String message, List<String> validFields, List<String> validOperators) {
        super(message);
        this.validFields = List.copyOf(validFields);
        this.validOperators = List.copyOf(validOperators);
    }

    public InvalidFilterException(String message, List<String> validFields, List<String> validOperators,
                                  Throwable cause) {
        super(message, cause);
        this.validFields = List.copyOf(validFields);
        this.validOperators = List.copyOf(validOperators);
    }

    public List<String> getValidFields() {
        return validFields;
    }

    public List<String> getValidOperators() {
        return validOperators;
    }
}
