package com.darle.engine.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.connection.reactive.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.reactive.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

@Configuration
public class RedisConfiguration {

    /**
     * Loads the atomic rate evaluation Lua script from the classpath resources.
     * Maps the return type to Long (1 for allowed, 0 for dropped).
     */
    @Bean
    public RedisScript<Long> rateLimiterScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource("scripts/rate_evaluator.lua"));
        script.setResultType(Long.class);
        return script;
    }

    /**
     * Provides a non-blocking, reactive Redis template utilizing String codecs
     * to eliminate payload overhead over Netty channels.
     */
    @Bean
    public ReactiveStringRedisTemplate reactiveStringRedisTemplate(
            ReactiveRedisConnectionFactory factory) {
        return new ReactiveStringRedisTemplate(factory);
    }
}
