package common.kafkas;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.TopicPartition;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

/**
 * Fallback error handler for every Kafka listener in the monorepo.
 *
 * - 3 retry attempts with 5-second fixed backoff (total ~15s wait before giving up)
 * - On give-up, publishes to {@code <original-topic>.DLT} on the same partition
 * - Operators monitor DLT topics via Grafana + alert when non-empty
 *
 * Why FixedBackOff over ExponentialBackOff: most transient failures here are
 * network blips to Keycloak/VNPT/SMTP that resolve within seconds. Exponential
 * (e.g. 1s → 2s → 4s) gives only ~7s total budget vs 15s — and a poison message
 * should fail in the first attempt anyway, we're not trying to outlast a long
 * outage, we're trying to survive a spike.
 *
 * The deprecated noop behavior before this class existed was "retry forever on
 * exception" — one poison message could freeze a consumer group indefinitely.
 */
@Slf4j
@AutoConfiguration
public class KafkaErrorHandlerAutoConfiguration {

    private static final long RETRY_INTERVAL_MS = 5_000L;
    private static final long MAX_ATTEMPTS = 3L;

    @Bean
    @ConditionalOnBean(KafkaTemplate.class)
    @ConditionalOnMissingBean(DefaultErrorHandler.class)
    public DefaultErrorHandler kafkaDefaultErrorHandler(KafkaTemplate<String, Object> template) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                template,
                (record, ex) -> {
                    String dltTopic = record.topic() + ".DLT";
                    log.error("[KAFKA-DLT] Routing to {} after exhausted retries: {}",
                            dltTopic, ex.getMessage());
                    return new TopicPartition(dltTopic, record.partition());
                });

        DefaultErrorHandler handler = new DefaultErrorHandler(
                recoverer, new FixedBackOff(RETRY_INTERVAL_MS, MAX_ATTEMPTS));

        // Non-retryable exceptions go straight to DLT (e.g. deserialization failures
        // where retrying won't help).
        handler.addNotRetryableExceptions(
                IllegalArgumentException.class,
                NullPointerException.class,
                org.springframework.kafka.support.serializer.DeserializationException.class);

        return handler;
    }
}
