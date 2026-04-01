package common.paginations.services;

import common.paginations.dtos.PageRequest;
import common.paginations.dtos.PageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.Optional;
import java.util.function.Supplier;

@Service
@Slf4j
@RequiredArgsConstructor
public class CachedPageService {

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public <T> PageResponse<T> getOrLoad(
            String namespace,
            PageRequest request,
            Duration ttl,
            TypeReference<PageResponse<T>> typeRef,
            Supplier<PageResponse<T>> loader
    ) {
        String key = buildKey(namespace, request);

        Optional<PageResponse<T>> cached = get(key, typeRef);
        if (cached.isPresent()) {
            log.debug("[Page Cache] HIT key={}", key);
            return cached.get();
        }

        log.debug("[Page Cache] MISS key={}", key);
        PageResponse<T> result = loader.get();

        put(key, result, ttl);
        return result;
    }

    public void evictAll(String namespace) {
        String pattern = buildNamespacePattern(namespace);
        try {
            var keys = redis.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                redis.delete(keys);
                log.info("[Page Cache] Evicted {} keys for namespace={}", keys.size(), namespace);
            }
        } catch (Exception e) {
            log.warn("[Page Cache] Evict failed namespace={}: {}", namespace, e.getMessage());
        }
    }

    public void evict(String namespace, PageRequest request) {
        String key = buildKey(namespace, request);
        redis.delete(key);
        log.debug("[Page Cache] Evicted key={}", key);
    }

    private <T> Optional<PageResponse<T>> get(String key, TypeReference<PageResponse<T>> typeRef) {
        try {
            String json = redis.opsForValue().get(key);
            if (json == null) return Optional.empty();
            return Optional.of(objectMapper.readValue(json, typeRef));
        } catch (Exception e) {
            log.warn("[Page Cache] Deserialize failed key={}: {}", key, e.getMessage());
            return Optional.empty();
        }
    }

    private <T> void put(String key, PageResponse<T> value, Duration ttl) {
        try {
            redis.opsForValue().set(key, objectMapper.writeValueAsString(value), ttl);
        } catch (Exception e) {
            log.warn("[Page Cache] Serialize failed key={}: {}", key, e.getMessage());
        }
    }

    private String buildKey(String namespace, PageRequest request) {
        String sort = request.sortBy() != null ? request.sortBy() : "default";
        String dir = request.sortDir() != null ? request.sortDir() : "DESC";
        return namespace + ":page:" + request.page()
                + ":" + request.validSize()
                + ":" + sort
                + ":" + dir;
    }

    private String buildNamespacePattern(String namespace) {
        return namespace + ":page:*";
    }
}
