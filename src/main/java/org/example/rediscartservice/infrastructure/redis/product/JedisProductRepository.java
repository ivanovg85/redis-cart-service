package org.example.rediscartservice.infrastructure.redis.product;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.example.rediscartservice.domain.model.product.Product;
import org.example.rediscartservice.domain.port.product.ProductRepository;
import org.example.rediscartservice.infrastructure.redis.RedisJsonMapper;
import org.example.rediscartservice.infrastructure.redis.SearchSanitizer;
import org.springframework.stereotype.Repository;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.json.Path2;
import redis.clients.jedis.search.Query;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class JedisProductRepository implements ProductRepository {

    private final JedisPooled jedis;
    private final ObjectMapper mapper;

    private static final String INDEX = "idx:products";

    @Override
    public Product save(Product product) {
        final String key = "product:" + product.getId();
        try {
            String productJson = mapper.writeValueAsString(product);
            jedis.jsonSet(key, Path2.ROOT_PATH, productJson);
            return product;
        } catch (Exception e) {
            throw new RuntimeException("Could not serialize product to JSON", e);
        }
    }

    @Override
    public Optional<Product> findById(String id) {
        final String key = "product:" + id;
        try {
            return RedisJsonMapper.readJson(jedis, key, Product.class, mapper);
        } catch (Exception e) {
            throw new RuntimeException("Could not read/deserialize product " + id, e);
        }
    }

    @Override
    public void deleteById(String id) {
        jedis.del("product:" + id);
    }

    @Override
    public List<Product> searchByName(String textQuery) {
        var query = new Query("@name:" + SearchSanitizer.sanitize(textQuery));
        var result = jedis.ftSearch(INDEX, query);
        return result.getDocuments().stream()
                .map(doc -> RedisJsonMapper.readJson(jedis, doc.getId(), Product.class, mapper))
                .flatMap(Optional::stream)
                .toList();
    }

    @Override
    public List<Product> searchByDescription(String textQuery) {
        var query = new Query("@description:" + SearchSanitizer.sanitize(textQuery));
        var result = jedis.ftSearch(INDEX, query);
        return result.getDocuments().stream()
                .map(doc -> RedisJsonMapper.readJson(jedis, doc.getId(), Product.class, mapper))
                .flatMap(Optional::stream)
                .toList();
    }

    @Override
    public List<Product> findAll(int offset, int limit) {
        int off = Math.max(0, offset);
        int lim = Math.min(Math.max(1, limit), 500); // cap to keep queries reasonable

        // Use RediSearch to page across all product docs
        Query query = new Query("*").limit(off, lim);
        var res = jedis.ftSearch(INDEX, query);

        if (res == null || res.getDocuments() == null || res.getDocuments().isEmpty()) {
            return List.of();
        }

        // Each doc id is the Redis JSON key (e.g., product:{id}); fetch and deserialize
        return res.getDocuments().stream()
                .map(doc -> readJson(doc.getId()))
                .flatMap(Optional::stream)
                .toList();
    }

    private Optional<Product> readJson(String key) {
        try {
            Object raw = jedis.jsonGet(key, redis.clients.jedis.json.Path2.ROOT_PATH);
            if (raw == null) return Optional.empty();
            String json = (raw instanceof String) ? (String) raw : String.valueOf(raw);
            return RedisJsonMapper.readJson(jedis, key, Product.class, mapper);
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
