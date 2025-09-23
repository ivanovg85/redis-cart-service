package org.example.rediscartservice.infrastructure.redis.product;

import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.search.IndexDefinition;
import redis.clients.jedis.search.IndexOptions;
import redis.clients.jedis.search.Schema;

/** Shared helper to (re)create the Product RediSearch index for tests. */
@Configuration
public class TestRedisIndexHelper {

    public static void createProductsIndex(JedisPooled jedis) {
        var schema = new Schema()
                .addTextField("$.name", 1.0).as("name")
                .addTextField("$.description", 1.0).as("description")
                .addTagField("$.sku").as("sku")
                .addNumericField("$.price").as("price");

        var def = new IndexDefinition(IndexDefinition.Type.JSON)
                .setPrefixes("product:");

        jedis.ftCreate("idx:products", IndexOptions.defaultOptions().setDefinition(def), schema);
    }
}
