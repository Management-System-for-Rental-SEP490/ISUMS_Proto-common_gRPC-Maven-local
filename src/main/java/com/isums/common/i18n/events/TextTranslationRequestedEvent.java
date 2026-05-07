package com.isums.common.i18n.events;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Request to translate a single piece of text into one or more target locales.
 * Emitted by any service that owns translatable entities and consumed by the
 * AI translation pipeline.
 *
 * <h3>Contract</h3>
 * <ul>
 *   <li>{@code resourceType} is a dotted identifier combining entity type and
 *       field — e.g. {@code "notification.title"}, {@code "issue-ticket.description"}.
 *       The AI-Service uses it to resolve a {@code TranslationPolicy}.</li>
 *   <li>{@code sourceLanguage} may be {@code null}, in which case AWS Translate
 *       auto-detects.</li>
 *   <li>{@code targetLanguages} must be non-empty. AI-Service emits one
 *       {@link TextTranslationResultEvent} per target, even on partial failure.</li>
 *   <li>{@code callbackTopic} is where the producer wants results delivered —
 *       typically {@code "text.translation.result.<service>"}.</li>
 *   <li>{@code requestId} is the correlation ID; it round-trips to the result
 *       unchanged so consumers can tie results back to the originating request.</li>
 * </ul>
 */
public record TextTranslationRequestedEvent(
        UUID requestId,
        String resourceType,
        UUID resourceId,
        String fieldName,
        String text,
        String sourceLanguage,
        List<String> targetLanguages,
        String translationIntent,
        Boolean customerFacing,
        Instant requestedAt,
        String callbackTopic
) {
    public static final String TOPIC = "text.translation.requested";
}
