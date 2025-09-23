package org.example.rediscartservice.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.JedisPooled;

@Configuration
public class RedisConfig {

    @Bean
    public JedisPooled jedisClient() {
        return new JedisPooled("localhost", 6379);
    }

    @Bean
    CommandLineRunner testRedis(JedisPooled jedis) {
        return args -> {
            String pong = jedis.ping();
            System.out.println("Redis is alive, PING response = " + pong);
        };
    }
}
