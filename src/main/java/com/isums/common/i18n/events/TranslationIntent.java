package com.isums.common.i18n.events;

/**
 * Canonical intent values that drive {@code TranslationPolicy} resolution in
 * AI-Service. Transmitted as the {@code translationIntent} field on
 * {@link TextTranslationRequestedEvent} — stored as a raw String on the wire
 * so new intents can be added without breaking older consumers.
 */
public final class TranslationIntent {

    /** End-user-visible copy (notifications, issue titles, banner names). Triggers formal tone + bedrock polish. */
    public static final String CUSTOMER_FACING_UI = "CUSTOMER_FACING_UI";

    /** Staff-facing notes, internal logs. Casual tone, no polish. */
    public static final String STAFF_INTERNAL = "STAFF_INTERNAL";

    /** Tenant reply / approval request surfaces — customer-facing but conversational. */
    public static final String CUSTOMER_REPLY = "CUSTOMER_REPLY";

    /** Legal / contract metadata. Translate but do NOT polish (risk of semantic drift). */
    public static final String LEGAL_REFERENCE = "LEGAL_REFERENCE";

    /** System-generated labels (status changes, audit messages). Casual, cacheable. */
    public static final String SYSTEM_LOG = "SYSTEM_LOG";

    private TranslationIntent() {}
}
