package com.isums.common.i18n;

import java.util.List;
import java.util.Set;

/** Canonical list of locales the platform serves content in. */
public final class SupportedLocales {

    public static final String VI = "vi";
    public static final String EN = "en";
    public static final String JA = "ja";

    public static final String DEFAULT = VI;

    public static final Set<String> ALL = Set.of(VI, EN, JA);
    public static final List<String> ORDERED = List.of(VI, EN, JA);

    private SupportedLocales() {}

    public static boolean isSupported(String code) {
        if (code == null) return false;
        String n = TranslationMap.normalizeLanguage(code);
        return n != null && ALL.contains(n);
    }
}
