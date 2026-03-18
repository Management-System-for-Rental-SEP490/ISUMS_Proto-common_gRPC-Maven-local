package common.kafkas;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.RedisTemplate;

@AutoConfiguration
public class IsumKafkaAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public KafkaListenerHelper kafkaListenerHelper() {
        return new KafkaListenerHelper();
    }

    @Bean
    @ConditionalOnMissingBean
    public IdempotencyService idempotencyService(
            RedisTemplate<String, String> redisTemplate) {
        return new IdempotencyService(redisTemplate);
    }
}