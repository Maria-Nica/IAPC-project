package com.edupulse.retrievalpre.filter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.Filter.ExpressionType;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SafeFilterParserTest {

    private static final List<String> FIELDS = List.of("doc_id", "source", "version", "is_latest", "page");
    private static final List<String> OPERATORS = List.of("eq", "ne", "in", "gt", "gte", "lt", "lte");

    private final SafeFilterParser parser = new SafeFilterParser(new FilterProperties(FIELDS, OPERATORS));

    // ---------- Success path ----------

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void noFilterFallsThroughToStandardFlow(String rawFilter) {
        assertThat(parser.parse(rawFilter)).isEmpty();
    }

    @Test
    void singleComparisonIsAccepted() {
        Filter.Expression expression = parser.parse("source == 'owasp-top10-2021.pdf'").orElseThrow();

        assertThat(expression.type()).isEqualTo(ExpressionType.EQ);
        assertThat(expression.left()).isEqualTo(new Filter.Key("source"));
        assertThat(expression.right()).isEqualTo(new Filter.Value("owasp-top10-2021.pdf"));
    }

    @Test
    void andFilterIsAccepted() {
        Filter.Expression expression = parser.parse("source == 'a.pdf' && page > 3").orElseThrow();

        assertThat(expression.type()).isEqualTo(ExpressionType.AND);
        assertThat(((Filter.Expression) expression.left()).type()).isEqualTo(ExpressionType.EQ);
        assertThat(((Filter.Expression) expression.right()).type()).isEqualTo(ExpressionType.GT);
    }

    @Test
    void orFilterIsAccepted() {
        Filter.Expression expression = parser.parse("version >= 2 || page <= 10").orElseThrow();

        assertThat(expression.type()).isEqualTo(ExpressionType.OR);
    }

    @Test
    void nestedAndOrFilterIsAccepted() {
        Filter.Expression expression =
                parser.parse("(is_latest == true && page >= 1) || doc_id == 'x'").orElseThrow();

        assertThat(expression.type()).isEqualTo(ExpressionType.OR);
        assertThat(((Filter.Expression) expression.left()).type()).isEqualTo(ExpressionType.AND);
    }

    @Test
    void inFilterWithListIsAccepted() {
        Filter.Expression expression = parser.parse("doc_id in ['a', 'b']").orElseThrow();

        assertThat(expression.type()).isEqualTo(ExpressionType.IN);
        assertThat(((Filter.Value) expression.right()).value()).isEqualTo(List.of("a", "b"));
    }

    @Test
    void booleanValueIsNormalizedToStringForQdrant() {
        Filter.Expression expression = parser.parse("is_latest == true").orElseThrow();

        assertThat(expression.right()).isEqualTo(new Filter.Value("true"));
    }

    // ---------- Failure path ----------

    @Test
    void unknownFieldIsRejectedWithValidFields() {
        assertThatThrownBy(() -> parser.parse("password == 'x'"))
                .isInstanceOf(InvalidFilterException.class)
                .hasMessageContaining("Field 'password' is not allowed")
                .satisfies(e -> assertThat(((InvalidFilterException) e).getValidFields()).isEqualTo(FIELDS));
    }

    @Test
    void unknownFieldHiddenInsideAndIsRejected() {
        assertThatThrownBy(() -> parser.parse("source == 'a.pdf' && secret == 1"))
                .isInstanceOf(InvalidFilterException.class)
                .hasMessageContaining("Field 'secret' is not allowed");
    }

    @ParameterizedTest
    @ValueSource(strings = {"doc_id nin ['a']", "NOT (page == 1)"})
    void operatorOutsideWhitelistIsRejected(String rawFilter) {
        assertThatThrownBy(() -> parser.parse(rawFilter))
                .isInstanceOf(InvalidFilterException.class)
                .hasMessageContaining("is not allowed");
    }

    @Test
    void wrongValueTypeIsRejected() {
        assertThatThrownBy(() -> parser.parse("page == 'three'"))
                .isInstanceOf(InvalidFilterException.class)
                .hasMessageContaining("Field 'page' expects a number value");
    }

    @Test
    void invalidBooleanIsRejected() {
        assertThatThrownBy(() -> parser.parse("is_latest == 'yes'"))
                .isInstanceOf(InvalidFilterException.class)
                .hasMessageContaining("expects true or false");
    }

    @ParameterizedTest
    @ValueSource(strings = {"===", "source ==", "page > > 3"})
    void malformedFilterIsRejected(String rawFilter) {
        assertThatThrownBy(() -> parser.parse(rawFilter))
                .isInstanceOf(InvalidFilterException.class)
                .hasMessageContaining("Malformed filter expression");
    }
}
