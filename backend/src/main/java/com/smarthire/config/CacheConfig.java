package com.smarthire.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.smarthire.common.cache.CacheNames;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

@Configuration
@EnableCaching
public class CacheConfig {

    private static final Logger log = LoggerFactory.getLogger(CacheConfig.class);

    @Bean
    @ConditionalOnProperty(name = "spring.cache.type", havingValue = "redis", matchIfMissing = true)
    public RedisCacheConfiguration redisCacheConfiguration(ObjectMapper objectMapper) {
        ObjectMapper cacheObjectMapper = objectMapper.copy();
        cacheObjectMapper.registerModule(new JavaTimeModule());
        cacheObjectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        cacheObjectMapper.activateDefaultTyping(
            BasicPolymorphicTypeValidator.builder()
                .allowIfSubType(Object.class)
                .build(),
            ObjectMapper.DefaultTyping.EVERYTHING,
            JsonTypeInfo.As.PROPERTY
        );

        GenericJackson2JsonRedisSerializer serializer =
            new GenericJackson2JsonRedisSerializer(cacheObjectMapper);

        return RedisCacheConfiguration.defaultCacheConfig()
            .disableCachingNullValues()
            .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer));
    }

    @Bean
    @ConditionalOnProperty(name = "spring.cache.type", havingValue = "redis", matchIfMissing = true)
    public RedisCacheManagerBuilderCustomizer redisCacheManagerBuilderCustomizer(
        RedisCacheConfiguration redisCacheConfiguration
    ) {
        return builder -> builder
            .cacheDefaults(redisCacheConfiguration.entryTtl(Duration.ofMinutes(10)))
            .withCacheConfiguration(
                CacheNames.JOB_DETAILS,
                redisCacheConfiguration.entryTtl(Duration.ofMinutes(30))
            )
            .withCacheConfiguration(
                CacheNames.JOB_LISTINGS,
                redisCacheConfiguration.entryTtl(Duration.ofMinutes(10))
            )
            .withCacheConfiguration(
                CacheNames.STATISTICS_OVERVIEW,
                redisCacheConfiguration.entryTtl(Duration.ofMinutes(5))
            );
    }

    @Bean
    public CacheErrorHandler cacheErrorHandler() {
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
                log.warn("Cache GET failed for cache={} key={}: {}", cacheName(cache), key, exception.getMessage());
            }

            @Override
            public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
                log.warn("Cache PUT failed for cache={} key={}: {}", cacheName(cache), key, exception.getMessage());
            }

            @Override
            public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
                log.warn("Cache EVICT failed for cache={} key={}: {}", cacheName(cache), key, exception.getMessage());
            }

            @Override
            public void handleCacheClearError(RuntimeException exception, Cache cache) {
                log.warn("Cache CLEAR failed for cache={}: {}", cacheName(cache), exception.getMessage());
            }

            private String cacheName(Cache cache) {
                return cache == null ? "unknown" : cache.getName();
            }
        };
    }
}
