package com.isums.common.i18n;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TranslationMapConverterTest {

    private final TranslationMapConverter converter = new TranslationMapConverter();
    private final ObjectMapper mapper = new ObjectMapper();
    private final TypeReference<Map<String, String>> mapType = new TypeReference<>() {};

    @Test
    void roundTripsAllKeys() throws Exception {
        TranslationMap m = TranslationMap.fromSource("vi", "Xin chào")
                .withTranslation("en", "Hello")
                .withTranslation("ja", "こんにちは")
                .mergeAutoFilled(Map.of());

        String serialized = converter.convertToDatabaseColumn(m);
        assertThat(serialized).isNotNull();
        Map<String, String> parsed = mapper.readValue(serialized, mapType);
        assertThat(parsed).containsEntry("vi", "Xin chào")
                .containsEntry("en", "Hello")
                .containsEntry("ja", "こんにちは")
                .containsEntry("_source", "vi");

        TranslationMap back = converter.convertToEntityAttribute(serialized);
        assertThat(back).isEqualTo(m);
    }

    @Test
    void writesNullForEmptyAttribute() {
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
        assertThat(converter.convertToDatabaseColumn(TranslationMap.empty())).isNull();
    }

    @Test
    void readsNullForBlankOrNullColumn() {
        assertThat(converter.convertToEntityAttribute(null)).isNull();
        assertThat(converter.convertToEntityAttribute("")).isNull();
        assertThat(converter.convertToEntityAttribute("   ")).isNull();
    }

    @Test
    void legacyPlainStringRowBecomesViSource() {
        TranslationMap parsed = converter.convertToEntityAttribute("Xin chào 2026");
        assertThat(parsed).isNotNull();
        assertThat(parsed.get("vi")).contains("Xin chào 2026");
        assertThat(parsed.getSource()).isEqualTo("vi");
    }

    @Test
    void malformedJsonFallsBackToViSource() {
        TranslationMap parsed = converter.convertToEntityAttribute("{not valid json");
        assertThat(parsed).isNotNull();
        assertThat(parsed.get("vi")).contains("{not valid json");
        assertThat(parsed.getSource()).isEqualTo("vi");
    }

    @Test
    void preservesAutoList() {
        TranslationMap m = TranslationMap.fromSource("vi", "Xin chào")
                .mergeAutoFilled(Map.of("en", "Hello", "ja", "こんにちは"));

        String serialized = converter.convertToDatabaseColumn(m);
        TranslationMap back = converter.convertToEntityAttribute(serialized);
        assertThat(back.getAutoLocales()).containsExactly("en", "ja");
    }
}
