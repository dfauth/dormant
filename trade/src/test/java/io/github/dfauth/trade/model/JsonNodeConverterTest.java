package io.github.dfauth.trade.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JsonNodeConverterTest {

    private final JsonNodeConverter converter = new JsonNodeConverter();

    // --- convertToDatabaseColumn ---

    @Test
    void convertToDatabaseColumn_null_returnsNull() {
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
    }

    @Test
    void convertToDatabaseColumn_object_returnsJsonString() {
        var node = new com.fasterxml.jackson.databind.ObjectMapper().createObjectNode();
        node.put("key", "value");
        // verify the converter uses the tools.jackson ObjectMapper internally
        var result = converter.convertToDatabaseColumn(
                new tools.jackson.databind.ObjectMapper().valueToTree(java.util.Map.of("key", "value"))
        );
        assertThat(result).contains("key").contains("value");
    }

    // --- convertToEntityAttribute ---

    @Test
    void convertToEntityAttribute_null_returnsNull() {
        assertThat(converter.convertToEntityAttribute(null)).isNull();
    }

    @Test
    void convertToEntityAttribute_validJson_returnsNode() {
        var node = converter.convertToEntityAttribute("{\"x\":42}");
        assertThat(node).isNotNull();
        assertThat(node.get("x").asInt()).isEqualTo(42);
    }

    @Test
    void convertToEntityAttribute_invalidJson_throwsRuntimeException() {
        assertThatThrownBy(() -> converter.convertToEntityAttribute("not valid json {{{"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to parse stored JSON value");
    }
}
