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

    public boolean isDuplicate(String messageId) {
        Boolean isNew = redisTemplate.opsForValue()
                .setIfAbsent(keyPrefix + ":" + messageId, "1", Duration.ofDays(ttlDays));
        return Boolean.FALSE.equals(isNew);
    }

    public void markProcessed(String messageId) {
        log.debug("Marked processed messageId={}", messageId);
    }
}