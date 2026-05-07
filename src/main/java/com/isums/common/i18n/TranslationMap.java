package com.isums.common.i18n;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

public final class TranslationMap {

    public static final String KEY_SOURCE = "_source";
    public static final String KEY_AUTO = "_auto";
    private static final String AUTO_DELIMITER = ",";

    private final LinkedHashMap<String, String> entries;

    @JsonCreator
    public TranslationMap(Map<String, String> raw) {
        this.entries = normalize(raw);
    }

    public TranslationMap() {
        this(Map.of());
    }

    public static TranslationMap empty() {
        return new TranslationMap();
    }

    public static TranslationMap fromSource(String sourceLang, String text) {
        String code = normalizeLanguage(sourceLang);
        if (code == null || text == null || text.isBlank()) {
            return empty();
        }
        LinkedHashMap<String, String> m = new LinkedHashMap<>();
        m.put(code, text.trim());
        m.put(KEY_SOURCE, code);
        return new TranslationMap(m);
    }

    public static TranslationMap of(String vi, String en, String ja) {
        LinkedHashMap<String, String> m = new LinkedHashMap<>();
        if (vi != null && !vi.isBlank()) m.put("vi", vi.trim());
        if (en != null && !en.isBlank()) m.put("en", en.trim());
        if (ja != null && !ja.isBlank()) m.put("ja", ja.trim());
        return new TranslationMap(m);
    }

    @JsonValue
    public Map<String, String> asMap() {
        return Collections.unmodifiableMap(entries);
    }

    public boolean isEmpty() {
        return languagesPresent().isEmpty();
    }

    public Set<String> languagesPresent() {
        Set<String> out = new LinkedHashSet<>();
        for (Map.Entry<String, String> e : entries.entrySet()) {
            if (isReserved(e.getKey())) continue;
            if (e.getValue() != null && !e.getValue().isBlank()) {
                out.add(e.getKey());
            }
        }
        return out;
    }

    public Set<String> missingLocales(Collection<String> required) {
        Set<String> want = new TreeSet<>();
        for (String r : required) {
            String n = normalizeLanguage(r);
            if (n != null) want.add(n);
        }
        Set<String> have = languagesPresent();
        want.removeAll(have);
        return want;
    }

    public Optional<String> get(String lang) {
        String code = normalizeLanguage(lang);
        if (code == null || isReserved(code)) return Optional.empty();
        String v = entries.get(code);
        return (v == null || v.isBlank()) ? Optional.empty() : Optional.of(v);
    }

    public String resolve(String preferredLanguage) {
        String pref = normalizeLanguage(preferredLanguage);
        if (pref != null) {
            String v = entries.get(pref);
            if (v != null && !v.isBlank()) return v;
        }
        String source = entries.get(KEY_SOURCE);
        if (source != null) {
            String v = entries.get(source);
            if (v != null && !v.isBlank()) return v;
        }
        for (Map.Entry<String, String> e : entries.entrySet()) {
            if (isReserved(e.getKey())) continue;
            if (e.getValue() != null && !e.getValue().isBlank()) return e.getValue();
        }
        return null;
    }

    @JsonIgnore
    public String getSource() {
        return entries.get(KEY_SOURCE);
    }

    @JsonIgnore
    public List<String> getAutoLocales() {
        String raw = entries.get(KEY_AUTO);
        if (raw == null || raw.isBlank()) return List.of();
        List<String> out = new ArrayList<>();
        for (String part : raw.split(AUTO_DELIMITER)) {
            String c = normalizeLanguage(part);
            if (c != null && !out.contains(c)) out.add(c);
        }
        return out;
    }

    public boolean isAuto(String lang) {
        String code = normalizeLanguage(lang);
        return code != null && getAutoLocales().contains(code);
    }

    public TranslationMap withTranslation(String lang, String text) {
        String code = normalizeLanguage(lang);
        if (code == null || isReserved(code)) return this;
        LinkedHashMap<String, String> copy = new LinkedHashMap<>(entries);
        if (text == null || text.isBlank()) {
            copy.remove(code);
            removeAuto(copy, code);
        } else {
            copy.put(code, text.trim());
            removeAuto(copy, code);
        }
        return new TranslationMap(copy);
    }

    public TranslationMap withSource(String lang) {
        String code = normalizeLanguage(lang);
        LinkedHashMap<String, String> copy = new LinkedHashMap<>(entries);
        if (code == null) {
            copy.remove(KEY_SOURCE);
        } else {
            copy.put(KEY_SOURCE, code);
        }
        return new TranslationMap(copy);
    }

    public TranslationMap mergeAutoFilled(Map<String, String> autoResults) {
        if (autoResults == null || autoResults.isEmpty()) return this;
        LinkedHashMap<String, String> copy = new LinkedHashMap<>(entries);
        Set<String> autoSet = new LinkedHashSet<>(getAutoLocales());
        boolean changed = false;
        for (Map.Entry<String, String> e : autoResults.entrySet()) {
            String code = normalizeLanguage(e.getKey());
            if (code == null || isReserved(code)) continue;
            String current = copy.get(code);
            if (current != null && !current.isBlank()) continue;
            String v = e.getValue();
            if (v == null || v.isBlank()) continue;
            copy.put(code, v.trim());
            autoSet.add(code);
            changed = true;
        }
        if (!changed) return this;
        copy.put(KEY_AUTO, String.join(AUTO_DELIMITER, autoSet));
        return new TranslationMap(copy);
    }

    private static void removeAuto(LinkedHashMap<String, String> map, String code) {
        String raw = map.get(KEY_AUTO);
        if (raw == null || raw.isBlank()) return;
        List<String> kept = new ArrayList<>();
        for (String part : raw.split(AUTO_DELIMITER)) {
            String c = normalizeLanguage(part);
            if (c != null && !c.equals(code) && !kept.contains(c)) kept.add(c);
        }
        if (kept.isEmpty()) {
            map.remove(KEY_AUTO);
        } else {
            map.put(KEY_AUTO, String.join(AUTO_DELIMITER, kept));
        }
    }

    private static LinkedHashMap<String, String> normalize(Map<String, String> in) {
        LinkedHashMap<String, String> out = new LinkedHashMap<>();
        if (in == null) return out;
        for (Map.Entry<String, String> e : in.entrySet()) {
            if (e.getKey() == null) continue;
            String key = e.getKey().trim();
            if (key.isEmpty()) continue;
            String value = e.getValue();
            if (value == null) continue;
            if (isReserved(key)) {

                String v = value.trim();
                if (!v.isEmpty()) out.put(key, KEY_AUTO.equals(key) ? canonicalizeAutoList(v) : v.toLowerCase(Locale.ROOT));
            } else {
                String code = normalizeLanguage(key);
                if (code == null) continue;
                String v = value.trim();
                if (v.isEmpty()) continue;
                out.put(code, v);
            }
        }
        return out;
    }

    private static String canonicalizeAutoList(String raw) {
        List<String> out = new ArrayList<>();
        for (String part : raw.split(AUTO_DELIMITER)) {
            String c = normalizeLanguage(part);
            if (c != null && !out.contains(c)) out.add(c);
        }
        return String.join(AUTO_DELIMITER, out);
    }

    private static boolean isReserved(String key) {
        return KEY_SOURCE.equals(key) || KEY_AUTO.equals(key);
    }

    public static String normalizeLanguage(String raw) {
        if (raw == null) return null;
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) return null;

        String base = trimmed.replace('_', '-');
        int dash = base.indexOf('-');
        if (dash > 0) base = base.substring(0, dash);
        return base.toLowerCase(Locale.ROOT);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TranslationMap other)) return false;
        return entries.equals(other.entries);
    }

    @Override
    public int hashCode() {
        return Objects.hash(entries);
    }

    @Override
    public String toString() {
        return "TranslationMap" + entries;
    }
}

