// src/main/java/org/example/rediscartservice/infrastructure/redis/cart/CartIndexBootstrap.java
package org.example.rediscartservice.infrastructure.redis.cart;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.search.IndexDefinition;
import redis.clients.jedis.search.IndexOptions;
import redis.clients.jedis.search.Schema;

/**
 * Creates a RediSearch index for cart item HASHes at app startup.
 * Key pattern indexed: cart:{sessionId}:item:{productId}
 * Fields:
 *  - session_id  (TAG)       — used to scope queries to a single session
 *  - product_id  (TAG)
 *  - name        (TEXT)
 *  - short_desc  (TEXT)
 *  - amount      (NUMERIC)
 *  - total_price (NUMERIC)
 * Safe to run repeatedly; ignores "Index already exists" errors.
 */
@Configuration
@RequiredArgsConstructor
public class CartIndexBootstrap {

    public static final String INDEX_NAME = "idx:cart_items";

    @Bean
    CommandLineRunner createCartIndex(JedisPooled jedis) {
        return args -> {
            try {
                Schema schema = new Schema()
                        .addTagField("session_id")
                        .addTagField("product_id")
                        .addTextField("name", 1.0)
                        .addTextField("short_desc", 1.0)
                        .addNumericField("amount")
                        .addNumericField("total_price");

                IndexDefinition def = new IndexDefinition(IndexDefinition.Type.HASH)
                        .setPrefixes("cart:");

                jedis.ftCreate(
                        INDEX_NAME,
                        IndexOptions.defaultOptions().setDefinition(def),
                        schema
                );
                System.out.println("✅ Created RediSearch index " + INDEX_NAME);
            } catch (Exception e) {
                String msg = e.getMessage() == null ? "" : e.getMessage();
                if (msg.contains("Index already exists") || msg.contains("already exists")) {
                    System.out.println("ℹ️ RediSearch index " + INDEX_NAME + " already exists");
                } else {
                    throw e;
                }
            }
        };
    }
}