package com.isums.common.i18n;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class TranslationMapTest {

    @Test
    void ofFactoryTrimsBlanksAndPreservesOrder() {
        TranslationMap m = TranslationMap.of(" Xin chào ", "", "こんにちは");
        assertThat(m.asMap()).containsOnlyKeys("vi", "ja");
        assertThat(m.get("vi")).contains("Xin chào");
        assertThat(m.get("en")).isEmpty();
        assertThat(m.get("ja")).contains("こんにちは");
        assertThat(m.isEmpty()).isFalse();
    }

    @Test
    void fromSourceMarksSourceLocale() {
        TranslationMap m = TranslationMap.fromSource("vi", "Xin chào");
        assertThat(m.getSource()).isEqualTo("vi");
        assertThat(m.get("vi")).contains("Xin chào");
    }

    @Test
    void fromSourceRejectsBlank() {
        assertThat(TranslationMap.fromSource("vi", "   ").isEmpty()).isTrue();
        assertThat(TranslationMap.fromSource("", "hello").isEmpty()).isTrue();
        assertThat(TranslationMap.fromSource(null, "hello").isEmpty()).isTrue();
    }

    @Test
    void normalisesLocaleCodes() {
        Map<String, String> raw = new LinkedHashMap<>();
        raw.put("VI", "Xin chào");
        raw.put("en-US", "Hello");
        raw.put(" ja-JP ", "こんにちは");
        raw.put("_source", "VI");

        TranslationMap m = new TranslationMap(raw);
        assertThat(m.languagesPresent()).containsExactly("vi", "en", "ja");
        assertThat(m.getSource()).isEqualTo("vi");
    }

    @Test
    void resolveFollowsPreferenceThenSourceThenFallback() {
        TranslationMap m = TranslationMap.fromSource("vi", "Xin chào")
                .withTranslation("en", "Hello")
                .withTranslation("ja", "こんにちは");

        assertThat(m.resolve("ja")).isEqualTo("こんにちは");
        assertThat(m.resolve("en")).isEqualTo("Hello");
        assertThat(m.resolve("fr")).isEqualTo("Xin chào");
    }

    @Test
    void resolveIgnoresBlankSourceEntry() {
        Map<String, String> raw = new LinkedHashMap<>();
        raw.put("vi", "");
        raw.put("en", "Hello");
        raw.put("_source", "vi");
        TranslationMap m = new TranslationMap(raw);

        assertThat(m.resolve("vi")).isEqualTo("Hello");
    }

    @Test
    void missingLocalesExcludesReservedKeys() {
        TranslationMap m = TranslationMap.fromSource("vi", "Xin chào");
        assertThat(m.missingLocales(Set.of("vi", "en", "ja"))).containsExactly("en", "ja");
    }

    @Test
    void mergeAutoFilledDoesNotOverwriteUserValues() {
        TranslationMap base = TranslationMap.fromSource("vi", "Xin chào")
                .withTranslation("en", "Hi there"); // user-entered en
        TranslationMap merged = base.mergeAutoFilled(Map.of(
                "en", "Hello",      // should NOT overwrite
                "ja", "こんにちは"   // should fill
        ));

        assertThat(merged.get("en")).contains("Hi there");
        assertThat(merged.get("ja")).contains("こんにちは");
        assertThat(merged.isAuto("en")).isFalse();
        assertThat(merged.isAuto("ja")).isTrue();
        assertThat(merged.isAuto("vi")).isFalse();
    }

    @Test
    void mergeAutoFilledWithNullOrEmptyIsNoop() {
        TranslationMap base = TranslationMap.fromSource("vi", "Xin chào");
        assertThat(base.mergeAutoFilled(null)).isEqualTo(base);
        assertThat(base.mergeAutoFilled(Map.of())).isEqualTo(base);
    }

    @Test
    void withTranslationOverridingAutoClearsAutoFlag() {
        TranslationMap m = TranslationMap.fromSource("vi", "Xin chào")
                .mergeAutoFilled(Map.of("en", "Hello", "ja", "こんにちは"));
        assertThat(m.isAuto("en")).isTrue();

        TranslationMap edited = m.withTranslation("en", "Howdy");
        assertThat(edited.isAuto("en")).isFalse();
        assertThat(edited.isAuto("ja")).isTrue();
    }

    @Test
    void withBlankTranslationRemovesLocale() {
        TranslationMap m = TranslationMap.of("Xin chào", "Hello", "こんにちは");
        TranslationMap cleared = m.withTranslation("en", "");
        assertThat(cleared.languagesPresent()).doesNotContain("en");
    }

    @Test
    void withSourceClearsWhenNull() {
        TranslationMap m = TranslationMap.fromSource("vi", "Xin chào");
        assertThat(m.getSource()).isEqualTo("vi");
        assertThat(m.withSource(null).getSource()).isNull();
    }

    @Test
    void autoListIsCanonicalAndDeduplicated() {
        Map<String, String> raw = new LinkedHashMap<>();
        raw.put("vi", "Xin chào");
        raw.put("en", "Hello");
        raw.put("_auto", "EN, ja-JP, en");
        TranslationMap m = new TranslationMap(raw);
        assertThat(m.getAutoLocales()).containsExactly("en", "ja");
    }

    @Test
    void asMapIsUnmodifiable() {
        TranslationMap m = TranslationMap.of("A", "B", "C");
        assertThat(m.asMap()).hasSize(3);
        org.junit.jupiter.api.Assertions.assertThrows(
                UnsupportedOperationException.class,
                () -> m.asMap().put("fr", "Bonjour"));
    }

    @Test
    void equalityIgnoresSerialisedOrderButRespectsContent() {
        TranslationMap a = TranslationMap.of("Xin", "Hello", "Hi");
        TranslationMap b = TranslationMap.of("Xin", "Hello", "Hi");
        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);

        TranslationMap c = TranslationMap.of("Xin", "Hello", "Different");
        assertThat(a).isNotEqualTo(c);
    }
}
