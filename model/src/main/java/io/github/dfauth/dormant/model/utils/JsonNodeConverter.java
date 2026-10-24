package io.github.dfauth.dormant.model.utils;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Converter
public class JsonNodeConverter implements AttributeConverter<JsonNode, String> {

    private static final JsonMapper MAPPER = new JsonMapper();

    @Override
    public String convertToDatabaseColumn(JsonNode attribute) {
        return attribute == null ? null : attribute.toString();
    }

    @Override
    public JsonNode convertToEntityAttribute(String dbData) {
        if (dbData == null)
            return null;
        return MAPPER.readTree(dbData);
    }
}
