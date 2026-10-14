package com.mymindmirror.backend.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import com.mymindmirror.backend.constants.CacheConstants;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * Redis cache configuration.
 *
 * <p><strong>Why {@code @SuppressWarnings({"deprecation", "removal"})}:</strong>
 * Spring Data Redis has deprecated the Jackson 2-based {@code GenericJackson2JsonRedisSerializer}
 * in favor of a Jackson 3 equivalent. MyMindMirror cannot migrate yet because
 * <strong>JJWT 0.11.5 does not support Jackson 3</strong> — it depends on Jackson 2 for JWT
 * claim parsing. Migrating one library without the other breaks JWT handling.
 *
 * <p>Tracked in the README's "Known Limitations" section. Remove this annotation and
 * migrate to the Jackson 3 serializer when JJWT ships Jackson 3 support.
 */
@Configuration
@EnableCaching
@SuppressWarnings({"deprecation", "removal"})
public class CacheConfig {

    @Bean
    public ObjectMapper redisObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        // Restrict polymorphic deserialization to known-safe packages.
        // Blocks every known Jackson gadget-chain class (com.sun.rowset.JdbcRowSetImpl,
        // org.apache.xalan.xsltc.trax.TemplatesImpl, etc.).
        BasicPolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType("com.mymindmirror.backend.")
                .allowIfSubType("java.lang.")
                .allowIfSubType("java.util.")
                .allowIfSubType("java.time.")
                .allowIfSubType("java.math.")
                .build();

        // EVERYTHING is required so Java records (which are final) carry @class info.
        objectMapper.activateDefaultTyping(
                ptv,
                ObjectMapper.DefaultTyping.EVERYTHING,
                JsonTypeInfo.As.PROPERTY
        );
        return objectMapper;
    }

    @Bean
    public org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer redisJsonSerializer(ObjectMapper redisObjectMapper) {
        return new org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer(redisObjectMapper);
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory,
                                                       org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer serializer) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(serializer);
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(serializer);
        template.afterPropertiesSet();

        return template;
    }

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory,
                                     org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer serializer) {

        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .disableCachingNullValues()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(config)
                .initialCacheNames(CacheConstants.allCacheNames())
                .build();
    }
}