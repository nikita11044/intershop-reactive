package practicum.intershopreactive.config.redis.serialization;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import practicum.intershopreactive.entity.Product;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

@Configuration
public class RedisSerializationConfig {

    @Bean
    public RedisCacheManagerBuilderCustomizer weatherCacheCustomizer(ObjectMapper objectMapper) {
        return builder -> builder.withCacheConfiguration(
                        "products",
                        RedisCacheConfiguration.defaultCacheConfig()
                                .entryTtl(Duration.of(5, ChronoUnit.MINUTES))
                                .serializeValuesWith(
                                        RedisSerializationContext.SerializationPair.fromSerializer(
                                                new Jackson2JsonRedisSerializer<>(Product.class)
                                        )
                                )
                )
                .withCacheConfiguration(
                        "productsList",
                        RedisCacheConfiguration.defaultCacheConfig()
                                .entryTtl(Duration.of(5, ChronoUnit.MINUTES))
                                .serializeValuesWith(
                                        RedisSerializationContext.SerializationPair.fromSerializer(
                                                new ProductListRedisSerializer(objectMapper)
                                        )
                                )
                )
                .withCacheConfiguration(
                        "cartItems",
                        RedisCacheConfiguration.defaultCacheConfig()
                                .entryTtl(Duration.of(5, ChronoUnit.MINUTES))
                                .serializeValuesWith(
                                        RedisSerializationContext.SerializationPair.fromSerializer(
                                                new CartItemsRedisSerializer(objectMapper)
                                        )
                                )
                );
    }
}
