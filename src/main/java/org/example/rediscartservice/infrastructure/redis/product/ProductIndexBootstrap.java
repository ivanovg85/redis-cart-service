package org.example.rediscartservice.infrastructure.redis.product;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.search.IndexDefinition;
import redis.clients.jedis.search.IndexOptions;
import redis.clients.jedis.search.Schema;

/**
 * Creates the RediSearch index for Product JSON documents at app startup.
 * Safe to run repeatedly; ignores "Index already exists" errors.
 */
@Configuration
@RequiredArgsConstructor
public class ProductIndexBootstrap {

    @Bean
    CommandLineRunner createProductIndex(JedisPooled jedis) {
        return args -> {
            try {
                Schema schema = new Schema()
                        .addTextField("$.name", 1.0).as("name")
                        .addTextField("$.description", 1.0).as("description")
                        .addTagField("$.sku").as("sku")
                        .addNumericField("$.price").as("price");

                IndexDefinition def = new IndexDefinition(IndexDefinition.Type.JSON)
                        .setPrefixes("product:");

                jedis.ftCreate(
                        "idx:products",
                        IndexOptions.defaultOptions().setDefinition(def),
                        schema
                );
                System.out.println("✅ Created RediSearch index idx:products");
            } catch (Exception e) {
                String msg = e.getMessage() == null ? "" : e.getMessage();
                if (msg.contains("Index already exists") || msg.contains("already exists")) {
                    System.out.println("ℹ️ RediSearch index idx:products already exists");
                } else {
                    throw e;
                }
            }
        };
    }
}
