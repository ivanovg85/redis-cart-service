// src/main/java/org/example/rediscartservice/infrastructure/redis/cart/JedisCartRepository.java
package org.example.rediscartservice.infrastructure.redis.cart;

import lombok.RequiredArgsConstructor;
import org.example.rediscartservice.domain.model.cart.CartItem;
import org.example.rediscartservice.domain.port.cart.CartRepository;
import org.springframework.stereotype.Repository;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.search.Document;
import redis.clients.jedis.search.Query;
import redis.clients.jedis.search.SearchResult;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Repository
@RequiredArgsConstructor
public class JedisCartRepository implements CartRepository {

    private static final String CART_INDEX = "idx:cart_items";      // created by CartIndexBootstrap
    private static final String COUNT_ZSET = "cart:idx:counts";      // member = cartId, score = distinct product count

    private final JedisPooled jedis;

    // ---------------------------------------------------------------------
    // Public API (port)
    // ---------------------------------------------------------------------

    @Override
    public List<CartItem> findBySession(String sessionId) {
        Objects.requireNonNull(sessionId, "sessionId must not be null");
        String cartId = cartIdForSession(sessionId);

        String itemsKey = keyCartItems(cartId);
        Set<String> productIds = jedis.smembers(itemsKey);
        if (productIds == null || productIds.isEmpty()) return List.of();

        List<CartItem> result = new ArrayList<>(productIds.size());
        for (String pid : productIds) {
            Map<String, String> cartItemHash = jedis.hgetAll(keyCartItem(cartId, pid));
            if (cartItemHash != null && !cartItemHash.isEmpty()) {
                result.add(toCartItem(pid, cartItemHash));
            }
        }
        return result;
    }

    @Override
    public void add(String sessionId, CartItem newItem) {
        Objects.requireNonNull(sessionId, "sessionId must not be null");
        Objects.requireNonNull(newItem, "newItem must not be null");

        String cartId = cartIdForSession(sessionId);
        String itemsKey = keyCartItems(cartId);
        String itemKey  = keyCartItem(cartId, newItem.getProductId());

        // Track membership; 1 means it was newly added to the set
        long added = jedis.sadd(itemsKey, newItem.getProductId());

        Map<String, String> existing = jedis.hgetAll(itemKey);
        boolean exists = existing != null && !existing.isEmpty();

        if (exists) {
            int existingAmount = parseInt(existing.get("amount"));
            BigDecimal existingTotal = parseMoney(existing.get("total_price"));

            int updatedAmount = existingAmount + newItem.getAmount();
            BigDecimal updatedTotal = existingTotal.add(newItem.getTotalPrice()).setScale(2, RoundingMode.HALF_UP);

            Map<String, String> payload = new HashMap<>();
            payload.put("cart_id", cartId);
            payload.put("product_id", newItem.getProductId());
            payload.put("name", newItem.getName());
            payload.put("short_desc", newItem.getShortDescription());
            payload.put("amount", String.valueOf(updatedAmount));
            payload.put("total_price", updatedTotal.toPlainString());

            jedis.hset(itemKey, payload);
        } else {
            Map<String, String> payload = new HashMap<>();
            payload.put("cart_id", cartId);
            payload.put("product_id", newItem.getProductId());
            payload.put("name", newItem.getName());
            payload.put("short_desc", newItem.getShortDescription());
            payload.put("amount", String.valueOf(newItem.getAmount()));
            payload.put("total_price", newItem.getTotalPrice().toPlainString());

            jedis.hset(itemKey, payload);
        }

        if (added == 1L) {
            jedis.zincrby(COUNT_ZSET, 1.0, sessionId);
        }
    }

    @Override
    public void remove(String sessionId, String productId) {
        Objects.requireNonNull(sessionId, "sessionId must not be null");
        Objects.requireNonNull(productId, "productId must not be null");

        String cartId = cartIdForSession(sessionId);
        String itemsKey = keyCartItems(cartId);
        String itemKey  = keyCartItem(cartId, productId);

        jedis.del(itemKey);
        long removed = jedis.srem(itemsKey, productId);
        if (removed == 1L) {
            double newScore = jedis.zincrby(COUNT_ZSET, -1.0, sessionId);
            if (newScore <= 0.0) {
                jedis.zrem(COUNT_ZSET, sessionId);
            }
        }
    }

    @Override
    public List<CartItem> searchByShortDescription(String sessionId, String query) {
        Objects.requireNonNull(sessionId, "sessionId must not be null");
        String term = query == null ? "" : query.trim();
        if (term.isEmpty()) {
            return findBySession(sessionId);
        }

        String cartId = cartIdForSession(sessionId);

        // Restrict to this cart and search by short_desc or name
        String escaped = escape(term);
        String redisQuery = String.format("@cart_id:{%s} (@short_desc:(%s*) | @name:(%s*))",
                cartId, escaped, escaped);

        Query q = new Query(redisQuery).limit(0, 200); // no scores by default
        SearchResult res = jedis.ftSearch(CART_INDEX, q);

        if (res == null || res.getDocuments() == null || res.getDocuments().isEmpty()) {
            return List.of();
        }

        List<CartItem> out = new ArrayList<>(res.getDocuments().size());
        for (Document doc : res.getDocuments()) {
            Map<String, Object> props = StreamSupport.stream(doc.getProperties().spliterator(), false)
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

            String pid = objToString(props.get("product_id"));
            Map<String, String> asHash = new HashMap<>();
            asHash.put("name", objToString(props.get("name")));
            asHash.put("short_desc", objToString(props.get("short_desc")));
            asHash.put("amount", objToString(props.get("amount")));
            asHash.put("total_price", objToString(props.get("total_price")));

            out.add(toCartItem(pid, asHash));
        }
        return out;
    }

    @Override
    public List<String> sessionsWithItemCountGreaterThan(int threshold) {
        String minExclusive = "(" + threshold;
        List<String> sessionIds = jedis.zrangeByScore(COUNT_ZSET, minExclusive, "+inf");
        return (sessionIds == null) ? List.of() : sessionIds;
    }

    @Override
    public void restoreFromPreviousSession(String username, String currentSessionId) {
        Objects.requireNonNull(username, "username must not be null");
        Objects.requireNonNull(currentSessionId, "currentSessionId must not be null");

        // Take the SECOND most recent session (index 1). If none → no-op.
        String keyUserSessions = keyUserSessions(username);
        List<String> second = jedis.zrevrange(keyUserSessions, 1, 1);
        if (second == null || second.isEmpty()) return;

        String previousSessionId = second.getFirst();
        if (previousSessionId == null || previousSessionId.isBlank()) return;

        // Read that session's cartId
        String previousCartId = jedis.hget(keySessionMeta(previousSessionId), "cart_id");
        if (previousCartId == null || previousCartId.isBlank()) return;

        // Rebind the CURRENT session to the previous cart (no copying)
        jedis.hset(keySessionMeta(currentSessionId), Map.of("cart_id", previousCartId));
    }

    // ---------------------------------------------------------------------
    // Key helpers
    // ---------------------------------------------------------------------

    private String keyCartItems(String cartId) { return "cart:" + cartId + ":items"; }
    private String keyCartItem(String cartId, String productId) { return "cart:" + cartId + ":item:" + productId; }
    private String keySessionMeta(String sessionId) { return "sess:" + sessionId + ":meta"; }
    private String keyUserSessions(String username) { return "sess:user:" + username; }

    // Ensures a cart_id is present for the session; allocates one if missing
    private String cartIdForSession(String sessionId) {
        String metaKey = keySessionMeta(sessionId);
        String cartId = jedis.hget(metaKey, "cart_id");
        if (cartId == null || cartId.isBlank()) {
            cartId = UUID.randomUUID().toString();
            jedis.hset(metaKey, Map.of("cart_id", cartId));
        }
        return cartId;
    }

    // ---------------------------------------------------------------------
    // Mapping / parsing helpers
    // ---------------------------------------------------------------------

    private CartItem toCartItem(String productId, Map<String, String> h) {
        String name = h.getOrDefault("name", "");
        String shortDesc = h.getOrDefault("short_desc", "");
        int amount = parseInt(h.get("amount"));
        BigDecimal total = parseMoney(h.get("total_price"));
        return CartItem.builder()
                .productId(productId)
                .name(name)
                .shortDescription(shortDesc)
                .amount(amount)
                .totalPrice(total)
                .build();
    }

    private int parseInt(String s) {
        try { return s == null ? 0 : Integer.parseInt(s); }
        catch (NumberFormatException e) { return 0; }
    }

    private BigDecimal parseMoney(String s) {
        try { return s == null ? BigDecimal.ZERO : new BigDecimal(s).setScale(2, RoundingMode.HALF_UP); }
        catch (NumberFormatException e) { return BigDecimal.ZERO; }
    }

    private String objToString(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    // Minimal RediSearch query escaping
    private String escape(String s) {
        if (s == null) return "";
        return s.replaceAll("([\\-\\[\\]\\{\\}\\(\\)\"~*?:\\\\/])", "\\\\$1").trim();
    }
}