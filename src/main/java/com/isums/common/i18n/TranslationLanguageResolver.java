package com.isums.common.i18n;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Locale;

/**
 * Resolves the caller's preferred language for the current request, in this
 * precedence order:
 * <ol>
 *   <li>{@code Accept-Language} header (first non-wildcard token)</li>
 *   <li>{@code locale} claim on the authenticated JWT principal</li>
 *   <li>Spring's {@link LocaleContextHolder}</li>
 *   <li>{@link SupportedLocales#DEFAULT}</li>
 * </ol>
 * Always returns one of {@link SupportedLocales#ALL}; unsupported codes fall
 * through to the default. No request context required — callable from any
 * thread.
 */
public class TranslationLanguageResolver {

    public String currentLanguage() {
        String header = fromHeader();
        if (header != null) return header;
        String jwt = fromJwt();
        if (jwt != null) return jwt;
        String ctx = fromLocaleContext();
        if (ctx != null) return ctx;
        return SupportedLocales.DEFAULT;
    }

    private String fromHeader() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs)) {
            return null;
        }
        HttpServletRequest req = attrs.getRequest();
        if (req == null) return null;
        String raw = req.getHeader("Accept-Language");
        if (raw == null || raw.isBlank()) return null;
        for (String part : raw.split(",")) {
            String token = part.split(";")[0].trim();
            if (token.isEmpty() || token.startsWith("*")) continue;
            String code = TranslationMap.normalizeLanguage(Locale.forLanguageTag(token).getLanguage());
            if (SupportedLocales.isSupported(code)) return code;
        }
        return null;
    }

    private String fromJwt() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return null;
        if (!(auth.getPrincipal() instanceof Jwt jwt)) return null;
        String claim = jwt.getClaimAsString("locale");
        if (claim == null) return null;
        String code = TranslationMap.normalizeLanguage(claim);
        return SupportedLocales.isSupported(code) ? code : null;
    }

    private String fromLocaleContext() {
        Locale locale = LocaleContextHolder.getLocale();
        if (locale == null) return null;
        String code = TranslationMap.normalizeLanguage(locale.getLanguage());
        return SupportedLocales.isSupported(code) ? code : null;
    }
}
