package com.isums.common.i18n.events;

import java.time.Instant;
import java.util.UUID;

/**
 * One translation outcome for a single {@code (resourceId, targetLanguage)}
 * pair. AI-Service emits one of these per target requested, so a consumer
 * that asked for two targets will receive two result events.
 *
 * <h3>Status values</h3>
 * <ul>
 *   <li>{@code DONE} — {@code translatedText} contains the translation.</li>
 *   <li>{@code SKIPPED} — source and target are the same language, text was
 *       blank, or policy rejected the pair; {@code translatedText} mirrors the
 *       input.</li>
 *   <li>{@code FAILED} — {@code errorMessage} describes why; {@code translatedText}
 *       is {@code null}. Consumers should keep the field empty rather than
 *       storing a garbage fallback.</li>
 * </ul>
 */
public record TextTranslationResultEvent(
        UUID requestId,
        String resourceType,
        UUID resourceId,
        String fieldName,
        String sourceLanguage,
        String targetLanguage,
        String translatedText,
        String provider,
        String status,
        String errorMessage,
        Instant translatedAt
) {
    public static final String STATUS_DONE = "DONE";
    public static final String STATUS_SKIPPED = "SKIPPED";
    public static final String STATUS_FAILED = "FAILED";
}
