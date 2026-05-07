package common.paginations.configurations;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import common.paginations.cache.CachedPageService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

@AutoConfiguration
@EnableCaching
@EnableConfigurationProperties(IsumCacheProperties.class)
@ConditionalOnClass(RedisConnectionFactory.class)
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
            IsumCacheProperties props,
            List<IsumCacheConfigurer> configurers) {

        com.fasterxml.jackson.databind.ObjectMapper redisMapper =
                new com.fasterxml.jackson.databind.ObjectMapper();
        redisMapper.registerModule(new JavaTimeModule());
        redisMapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        redisMapper.activateDefaultTyping(
                com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator.builder()
                        .allowIfBaseType(Object.class)
                        .build(),
                com.fasterxml.jackson.databind.ObjectMapper.DefaultTyping.EVERYTHING,
                JsonTypeInfo.As.PROPERTY
        );

        RedisSerializer<Object> valueSerializer = new RedisSerializer<>() {
            @Override
            public byte[] serialize(Object value) throws SerializationException {
                if (value == null) return new byte[0];
                try {
                    return redisMapper.writeValueAsBytes(value);
                } catch (Exception e) {
                    throw new SerializationException("Serialize failed: " + e.getMessage(), e);
                }
            }

            @Override
            public Object deserialize(byte[] bytes) throws SerializationException {
                if (bytes == null || bytes.length == 0) return null;
                try {
                    return redisMapper.readValue(bytes, Object.class);
                } catch (Exception e) {
                    throw new SerializationException("Deserialize failed: " + e.getMessage(), e);
                }
            }
        };

        RedisCacheConfiguration base = RedisCacheConfiguration.defaultCacheConfig()
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(valueSerializer))
                .disableCachingNullValues()
                .entryTtl(props.getDefaultTtl());

        RedisCacheManager.RedisCacheManagerBuilder builder =
                RedisCacheManager.builder(cf).cacheDefaults(base);

        configurers.forEach(c -> c.cacheTtls().forEach((name, ttl) ->
                builder.withCacheConfiguration(name, base.entryTtl(ttl))));

        props.getNamespaces().forEach((name, ttl) ->
                builder.withCacheConfiguration(name, base.entryTtl(ttl)));

        return builder.build();
    }

    @Bean("isumPageObjectMapper")
    public ObjectMapper isumPageObjectMapper() {
        return JsonMapper.builder()
                .findAndAddModules()
                .build();
    }

    @Bean
    @ConditionalOnMissingBean
    public CachedPageService cachedPageService(
            StringRedisTemplate redis,
            @Qualifier("isumPageObjectMapper") ObjectMapper objectMapper,
            IsumCacheProperties cacheProperties) {
        return new CachedPageService(redis, objectMapper, cacheProperties);
    }
}