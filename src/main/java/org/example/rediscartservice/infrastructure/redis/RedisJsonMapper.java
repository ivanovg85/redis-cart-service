// src/main/java/org/example/rediscartservice/infrastructure/redis/RedisJsonMapper.java
package org.example.rediscartservice.infrastructure.redis;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.json.Path2;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class RedisJsonMapper {

    private RedisJsonMapper() {}

    // ---------- High-level helpers ----------

    public static <T> Optional<T> readJson(JedisPooled jedis, String key, Class<T> type, ObjectMapper mapper) {
        try {
            Object raw = jedis.jsonGet(key, Path2.ROOT_PATH);
            return toDomain(raw, type, mapper);
        } catch (Exception e) {
            throw new RuntimeException("Could not read key " + key + " from Redis", e);
        }
    }

    public static <T> Optional<T> readJson(JedisPooled jedis, String key, TypeReference<T> typeRef, ObjectMapper mapper) {
        try {
            Object raw = jedis.jsonGet(key, Path2.ROOT_PATH);
            return toDomain(raw, typeRef, mapper);
        } catch (Exception e) {
            throw new RuntimeException("Could not read key " + key + " from Redis", e);
        }
    }

    // ---------- Generic conversion ----------

    public static <T> Optional<T> toDomain(Object raw, Class<T> type, ObjectMapper mapper) {
        try {
            JsonNode node = normalize(raw, mapper);
            if (node == null || node.isNull()) return Optional.empty();
            return Optional.of(mapper.treeToValue(node, type));
        } catch (Exception e) {
            throw new RuntimeException("Failed to map RedisJSON to " + type.getSimpleName(), e);
        }
    }

    public static <T> Optional<T> toDomain(Object raw, TypeReference<T> typeRef, ObjectMapper mapper) {
        try {
            JsonNode node = normalize(raw, mapper);
            if (node == null || node.isNull()) return Optional.empty();
            return Optional.of(mapper.readValue(mapper.treeAsTokens(node), typeRef));
        } catch (Exception e) {
            throw new RuntimeException("Failed to map RedisJSON to " + typeRef.getType(), e);
        }
    }

    // ---------- Normalization ----------

    /**
     * Normalize RedisJSON return shapes into a JsonNode:
     *  - String → parse
     *  - List<?> → take first element (recursive normalize)
     *  - Map<?,?> → valueToTree
     *  - com.google.gson.JsonElement / org.json types → parse via toString()
     *  - Fallback: toString() only if it looks like JSON ("{" or "[")
     *  - Unwrap single-element arrays [ { ... } ]
     */
    private static JsonNode normalize(Object raw, ObjectMapper mapper) throws Exception {
        if (raw == null) return null;

        JsonNode node = null;

        if (raw instanceof String s) {
            node = mapper.readTree(s);

        } else if (raw instanceof List<?> list) {
            if (list.isEmpty()) return null;
            // normalize first element (handles [$root] pattern)
            return normalize(list.get(0), mapper);

        } else if (raw instanceof Map<?, ?> map) {
            node = mapper.valueToTree(map);

        } else if (isGsonElement(raw)) {
            node = mapper.readTree(raw.toString()); // Gson's toString() is JSON

        } else if (isOrgJson(raw)) {
            node = mapper.readTree(raw.toString()); // org.json toString() is JSON

        } else {
            // Last resort: try toString() only if it looks like JSON
            String s = raw.toString();
            if (looksLikeJson(s)) {
                node = mapper.readTree(s);
            } else {
                // Unknown non-JSON object (e.g., some internal holder) – treat as absent
                return null;
            }
        }

        // Unwrap single-element array returned for "$" path
        if (node.isArray() && node.size() == 1) {
            node = node.get(0);
        }
        return node;
    }

    // ---------- helpers ----------

    private static boolean looksLikeJson(String s) {
        if (s == null) return false;
        String t = s.trim();
        return (t.startsWith("{") && t.endsWith("}")) || (t.startsWith("[") && t.endsWith("]"));
    }

    // Avoid compile-time deps; check by class name
    private static boolean isGsonElement(Object o) {
        return o != null && o.getClass().getName().equals("com.google.gson.JsonElement")
                || (o != null && o.getClass().getName().startsWith("com.google.gson.") && hasSuperclass(o, "com.google.gson.JsonElement"));
    }

    private static boolean isOrgJson(Object o) {
        if (o == null) return false;
        String n = o.getClass().getName();
        return n.equals("org.json.JSONObject") || n.equals("org.json.JSONArray");
    }

    private static boolean hasSuperclass(Object o, String fqcn) {
        Class<?> c = o.getClass().getSuperclass();
        while (c != null) {
            if (c.getName().equals(fqcn)) return true;
            c = c.getSuperclass();
        }
        return false;
    }
}