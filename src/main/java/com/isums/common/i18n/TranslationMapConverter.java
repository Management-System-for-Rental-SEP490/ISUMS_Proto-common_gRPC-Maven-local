package com.isums.common.i18n;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * JPA {@link AttributeConverter} that stores a {@link TranslationMap} as a
 * flat JSON object in a TEXT/JSONB column. Legacy rows that predate this
 * converter (plain Vietnamese strings not wrapped in JSON) are accepted and
 * surfaced as {@code {"vi": <text>, "_source": "vi"}} to keep reads working.
 *
 * <p>Uses {@code autoApply = false} so each entity explicitly declares the
 * conversion on each column. This prevents accidental application to String
 * fields that look maplike but aren't translations.
 */
@Converter(autoApply = false)
public class TranslationMapConverter implements AttributeConverter<TranslationMap, String> {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<LinkedHashMap<String, String>> MAP_TYPE = new TypeReference<>() {};

    @Override
    public String convertToDatabaseColumn(TranslationMap attribute) {
        if (attribute == null || attribute.asMap().isEmpty()) {
            return null;
        }
        try {
            return MAPPER.writeValueAsString(attribute.asMap());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize TranslationMap", e);
        }
    }

    @Override
    public TranslationMap convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }
        String trimmed = dbData.trim();
        if (!trimmed.startsWith("{")) {
            return TranslationMap.fromSource("vi", trimmed);
        }
        try {
            Map<String, String> raw = MAPPER.readValue(trimmed, MAP_TYPE);
            return new TranslationMap(raw);
        } catch (Exception e) {
            return TranslationMap.fromSource("vi", trimmed);
        }
    }
}
