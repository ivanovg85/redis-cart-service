package org.example.rediscartservice.web.cart;

import org.example.rediscartservice.application.cart.CartService;
import org.example.rediscartservice.config.CartIdleProperties;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import redis.clients.jedis.JedisPooled;

@TestConfiguration
public class CartControllerTestConfig {

    @Bean
    CartService cartService() {
        return Mockito.mock(CartService.class);
    }

    // SessionTouchAspect dependencies
    @Bean
    JedisPooled jedisPooled() {
        return Mockito.mock(JedisPooled.class);
    }

    @Bean
    CartIdleProperties cartIdleProperties() {
        CartIdleProperties properties = new CartIdleProperties();
        properties.setIdleTtl(java.time.Duration.ofSeconds(30));
        return properties;
    }
}