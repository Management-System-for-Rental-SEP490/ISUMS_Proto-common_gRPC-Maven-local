package common.kafkas;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Duration;

@Slf4j
@RequiredArgsConstructor
public class IdempotencyService {

    private final RedisTemplate<String, String> redisTemplate;

    @Value("${isums.kafka.idempotency.key-prefix:kafka:processed}")
    private String keyPrefix;

    @Value("${isums.kafka.idempotency.ttl-days:7}")
    private long ttlDays;

    /**
     * Read-only duplicate check. Returns true iff the message was previously
     * marked via {@link #markProcessed}.
     *
     * The previous implementation used setIfAbsent, which atomically marked
     * the message on first observation. That was wrong — if the handler
     * threw (e.g. template variable mismatch, transient DB error), Kafka
     * would re-deliver the record but isDuplicate would now return true and
     * the listener silently skipped it, so failed events were never retried.
     * Separating read from write means a failed handler leaves the key
     * unset and the retry actually runs.
     */
    public boolean isDuplicate(String messageId) {
        Boolean exists = redisTemplate.hasKey(keyPrefix + ":" + messageId);
        return Boolean.TRUE.equals(exists);
    }

    /** Mark after successful processing so subsequent deliveries skip. */
    public void markProcessed(String messageId) {
        redisTemplate.opsForValue()
                .set(keyPrefix + ":" + messageId, "1", Duration.ofDays(ttlDays));
        log.debug("Marked processed messageId={}", messageId);
    }
}