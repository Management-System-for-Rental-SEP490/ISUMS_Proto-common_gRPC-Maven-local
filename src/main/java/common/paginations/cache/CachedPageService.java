package common.paginations.cache;

import common.paginations.configurations.IsumCacheProperties;
import common.paginations.dtos.PageRequest;
import common.paginations.dtos.PageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

@Slf4j
@RequiredArgsConstructor
@SuppressWarnings("unused")
public class CachedPageService {

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final IsumCacheProperties props;

    public <T> PageResponse<T> getOrLoad(
            String namespace,
            PageRequest request,
            TypeReference<PageResponse<T>> typeRef,
            Supplier<PageResponse<T>> loader) {

        String key = CacheKeyBuilder.build(namespace, request);
        Duration ttl = props.getTtlFor(namespace);

        return get(key, typeRef)
                .map(cached -> {
                    log.debug("[PageCache] HIT  key={}", key);
                    return cached;
                })
                .orElseGet(() -> {
                    log.debug("[PageCache] MISS key={}", key);
                    PageResponse<T> result = loader.get();
                    put(key, result, ttl);
                    return result;
                });
    }

    public void evictAll(String namespace) {
        String pattern = CacheKeyBuilder.scanPattern(namespace);
        try {
            List<String> keys = scan(pattern);
            if (!keys.isEmpty()) {
                redis.delete(keys);
                log.info("[PageCache] Evicted {} keys namespace={}", keys.size(), namespace);
            }
        } catch (Exception e) {
            log.warn("[PageCache] EvictAll failed namespace={}: {}", namespace, e.getMessage());
        }
    }

    public void evict(String namespace, PageRequest request) {
        String key = CacheKeyBuilder.build(namespace, request);
        try {
            redis.delete(key);
            log.debug("[PageCache] Evicted key={}", key);
        } catch (Exception e) {
            log.warn("[PageCache] Evict failed key={}: {}", key, e.getMessage());
        }
    }

    private <T> Optional<PageResponse<T>> get(String key, TypeReference<PageResponse<T>> typeRef) {
        try {
            String json = redis.opsForValue().get(key);
            if (json == null) return Optional.empty();
            return Optional.of(objectMapper.readValue(json, typeRef));
        } catch (Exception e) {
            log.warn("[PageCache] Deserialize failed key={}: {}", key, e.getMessage());
            return Optional.empty();
        }
    }

    private <T> void put(String key, PageResponse<T> value, Duration ttl) {
        try {
            String json = objectMapper.writeValueAsString(value);
            redis.opsForValue().set(key, json, ttl);
            log.debug("[PageCache] Stored key={} ttl={}", key, ttl);
        } catch (Exception e) {
            log.warn("[PageCache] Serialize failed key={}: {}", key, e.getMessage());
        }
    }

    private List<String> scan(String pattern) {
        List<String> keys = new ArrayList<>();
        ScanOptions opts = ScanOptions.scanOptions().match(pattern).count(200).build();
        redis.execute((RedisConnection connection) -> {
            try (Cursor<byte[]> cursor = connection.keyCommands().scan(opts)) {
                cursor.forEachRemaining(k ->
                        keys.add(new String(k, StandardCharsets.UTF_8)));
            } catch (Exception e) {
                log.warn("[PageCache] SCAN error pattern={}: {}", pattern, e.getMessage());
            }
            return null;
        });
        return keys;
    }
}