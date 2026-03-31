package common.paginations.configurations;

import common.paginations.services.CachedPageService;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;

@AutoConfiguration
@EnableCaching
@ConditionalOnClass(RedisConnectionFactory.class)
@ConditionalOnBean(RedisConnectionFactory.class)
public class IsumRedisAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory cf) {
        return new StringRedisTemplate(cf);
    }

    @Bean
    @ConditionalOnMissingBean
    public RedisCacheManager cacheManager(
            RedisConnectionFactory cf,
            java.util.Optional<IsumCacheConfigurer> configurer) {

        org.springframework.data.redis.serializer.GenericToStringSerializer<Object> valueSerializer =
                new org.springframework.data.redis.serializer.GenericToStringSerializer<>(Object.class);

        RedisCacheConfiguration base = RedisCacheConfiguration.defaultCacheConfig()
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .disableCachingNullValues()
                .entryTtl(Duration.ofMinutes(5));

        RedisCacheManager.RedisCacheManagerBuilder builder =
                RedisCacheManager.builder(cf).cacheDefaults(base);

        configurer.ifPresent(c ->
                c.cacheTtls().forEach((name, ttl) ->
                        builder.withCacheConfiguration(name, base.entryTtl(ttl))));

        return builder.build();
    }

    @Bean
    @ConditionalOnMissingBean
    public ObjectMapper objectMapper() {
        return JsonMapper.builder().build();
    }

    @Bean
    @ConditionalOnMissingBean
    public CachedPageService cachedPageService(
            StringRedisTemplate redis,
            ObjectMapper objectMapper) {
        return new CachedPageService(redis, objectMapper);
    }
}