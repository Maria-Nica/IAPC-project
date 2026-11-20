package com.edupulse.retrievalpre.filter;

import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionTextParser;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Turns the "filter" URL parameter into a validated {@link Filter.Expression}.
 * <p>
 * The text is parsed into an expression tree, then every node is checked against the whitelist:
 * only allowed fields, allowed comparison operators, AND/OR combinations and values of the
 * expected type are accepted. Only the validated object reaches the vector store, never the raw string.
 */
@Component
public class SafeFilterParser {

    // Expected value type of each metadata field (see docs/metadata-schema.md)
    private static final Map<String, Class<?>> FIELD_TYPES = Map.of(
            "doc_id", String.class,
            "source", String.class,
            "version", Number.class,
            "page", Number.class,
            "is_latest", Boolean.class);

    private final FilterExpressionTextParser textParser = new FilterExpressionTextParser();
    private final List<String> allowedFields;
    private final List<String> allowedOperators;

    public SafeFilterParser(FilterProperties properties) {
        this.allowedFields = properties.allowedFields();
        this.allowedOperators = properties.allowedOperators().stream()
                .map(operator -> operator.toLowerCase(Locale.ROOT))
                .toList();
    }

    /**
     * @param rawFilter filter text from the URL, e.g. {@code source == 'owasp-top10-2021.pdf' && page > 3}
     * @return the validated expression, or empty when no filter was given (standard flow)
     * @throws InvalidFilterException when the filter is malformed or not allowed
     */
    public Optional<Filter.Expression> parse(String rawFilter) {
        if (rawFilter == null || rawFilter.isBlank()) {
            return Optional.empty();
        }

        Filter.Expression expression;
        try {
            expression = textParser.parse(rawFilter);
        } catch (RuntimeException e) {
            throw invalid("Malformed filter expression", e);
        }

        return Optional.of(validate(expression));
    }

    public List<String> getAllowedFields() {
        return allowedFields;
    }

    public List<String> getAllowedOperators() {
        return allowedOperators;
    }

    // Validates the expression tree and returns a rebuilt copy with normalized values
    private Filter.Expression validate(Filter.Operand operand) {
        if (operand instanceof Filter.Group group) {
            return validate(group.content());
        }
        if (!(operand instanceof Filter.Expression expression)) {
            throw invalid("Unexpected filter element: " + operand, null);
        }

        return switch (expression.type()) {
            // The tree structure already keeps the precedence of AND/OR, so groups are not needed
            case AND, OR -> new Filter.Expression(expression.type(),
                    validate(expression.left()),
                    validate(expression.right()));
            default -> validateComparison(expression);
        };
    }

    private Filter.Expression validateComparison(Filter.Expression expression) {
        String operator = expression.type().name().toLowerCase(Locale.ROOT);
        if (!allowedOperators.contains(operator)) {
            throw invalid("Operator '" + operator + "' is not allowed", null);
        }
        if (!(expression.left() instanceof Filter.Key key)) {
            throw invalid("The left side of '" + operator + "' must be a field name", null);
        }
        if (!allowedFields.contains(key.key())) {
            throw invalid("Field '" + key.key() + "' is not allowed", null);
        }
        if (!(expression.right() instanceof Filter.Value value)) {
            throw invalid("The right side of '" + key.key() + "' must be a value", null);
        }
        Object normalizedValue = validateValue(key.key(), operator, value.value());
        return new Filter.Expression(expression.type(), new Filter.Key(key.key()), new Filter.Value(normalizedValue));
    }

    private Object validateValue(String field, String operator, Object value) {
        Class<?> expectedType = FIELD_TYPES.getOrDefault(field, String.class);

        if ("in".equals(operator)) {
            if (!(value instanceof Collection<?> values) || values.isEmpty()) {
                throw invalid("Operator 'in' on '" + field + "' needs a non-empty list, e.g. ['a', 'b']", null);
            }
            return values.stream().map(item -> checkType(field, expectedType, item)).toList();
        }
        return checkType(field, expectedType, value);
    }

    private Object checkType(String field, Class<?> expectedType, Object value) {
        if (expectedType == Boolean.class) {
            // Spring AI's Qdrant filter converter only supports string and number values,
            // so booleans are stored and matched as the strings "true" / "false" (see docs/metadata-schema.md)
            if (value instanceof Boolean bool) {
                return bool.toString();
            }
            if ("true".equals(value) || "false".equals(value)) {
                return value;
            }
            throw invalid("Field '" + field + "' expects true or false, got: " + value, null);
        }
        if (!expectedType.isInstance(value)) {
            throw invalid("Field '" + field + "' expects a " + expectedType.getSimpleName().toLowerCase(Locale.ROOT)
                    + " value, got: " + value, null);
        }
        return value;
    }

    private InvalidFilterException invalid(String message, Throwable cause) {
        return new InvalidFilterException(message, allowedFields, allowedOperators, cause);
    }
}
