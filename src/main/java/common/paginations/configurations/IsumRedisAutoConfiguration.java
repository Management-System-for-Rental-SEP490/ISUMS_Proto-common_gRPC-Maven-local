package common.paginations.configurations;

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

        RedisCacheConfiguration base = RedisCacheConfiguration.defaultCacheConfig()
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
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