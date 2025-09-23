// src/test/java/org/example/rediscartservice/infrastructure/redis/cart/JedisCartRepositoryTest.java
package org.example.rediscartservice.infrastructure.redis.cart;

import org.example.rediscartservice.domain.model.cart.CartItem;
import org.example.rediscartservice.domain.port.cart.CartRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.search.Document;
import redis.clients.jedis.search.Query;
import redis.clients.jedis.search.SearchResult;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class JedisCartRepositoryTest {

    private static final String COUNT_ZSET = "cart:idx:counts";
    private static final String CART_INDEX = "idx:cart_items";

    private JedisPooled jedis;
    private CartRepository repository;

    @BeforeEach
    void setUp() {
        jedis = mock(JedisPooled.class);
        repository = new JedisCartRepository(jedis);
    }

    // ---------- findBySession ----------

    @Test
    void findBySession_returns_cart_items_for_resolved_cartId() {
        String sessionId = "sid-1";
        String cartId = "c-111";
        String metaKey = "sess:" + sessionId + ":meta";
        String itemsKey = "cart:" + cartId + ":items";
        String itemKey1 = "cart:" + cartId + ":item:p-1";
        String itemKey2 = "cart:" + cartId + ":item:p-2";

        when(jedis.hget(metaKey, "cart_id")).thenReturn(cartId);
        when(jedis.smembers(itemsKey)).thenReturn(Set.of("p-1", "p-2"));
        when(jedis.hgetAll(itemKey1)).thenReturn(Map.of(
                "name", "Mug",
                "short_desc", "Ceramic",
                "amount", "2",
                "total_price", "19.98"
        ));
        when(jedis.hgetAll(itemKey2)).thenReturn(Map.of(
                "name", "Bottle",
                "short_desc", "Steel 750ml",
                "amount", "1",
                "total_price", "24.99"
        ));

        var items = repository.findBySession(sessionId);

        assertThat(items).hasSize(2);
        var byId = items.stream().collect(Collectors.toMap(CartItem::getProductId, x -> x));
        assertThat(byId.get("p-1").getName()).isEqualTo("Mug");
        assertThat(byId.get("p-1").getAmount()).isEqualTo(2);
        assertThat(byId.get("p-1").getTotalPrice()).isEqualByComparingTo("19.98");
        assertThat(byId.get("p-2").getShortDescription()).isEqualTo("Steel 750ml");
        assertThat(byId.get("p-2").getTotalPrice()).isEqualByComparingTo("24.99");
    }

    // ---------- add ----------

    @Test
    void add_new_product_creates_hash_adds_membership_and_increments_count_index_by_sessionId() {
        String sessionId = "sid-2";
        String cartId = "c-222";
        String metaKey  = "sess:" + sessionId + ":meta";
        String itemsKey = "cart:" + cartId + ":items";
        String itemKey  = "cart:" + cartId + ":item:p-9";
        String countIdx = "cart:idx:counts";

        when(jedis.hget(metaKey, "cart_id")).thenReturn(cartId);
        when(jedis.sadd(itemsKey, "p-9")).thenReturn(1L);   // newly added
        when(jedis.hgetAll(itemKey)).thenReturn(Map.of());  // no existing hash

        CartItem item = CartItem.builder()
                .productId("p-9")
                .name("Notebook")
                .shortDescription("A5 dotted")
                .amount(3)
                .totalPrice(new BigDecimal("14.97"))
                .build();

        repository.add(sessionId, item);

        verify(jedis).hget(metaKey, "cart_id");   // from cartIdForSession(...)
        verify(jedis).sadd(itemsKey, "p-9");
        verify(jedis).hgetAll(itemKey);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String,String>> cap = ArgumentCaptor.forClass((Class) Map.class);
        verify(jedis).hset(eq(itemKey), cap.capture());
        Map<String,String> written = cap.getValue();
        assertThat(written).containsEntry("cart_id", cartId);
        assertThat(written).containsEntry("product_id", "p-9");
        assertThat(written).containsEntry("name", "Notebook");
        assertThat(written).containsEntry("short_desc", "A5 dotted");
        assertThat(written).containsEntry("amount", "3");
        assertThat(written).containsEntry("total_price", "14.97");

        verify(jedis).zincrby(countIdx, 1.0, sessionId);
        verifyNoMoreInteractions(jedis);
    }

    @Test
    void add_existing_product_increments_amount_and_total_without_touching_count_index() {
        String sessionId = "sid-3";
        String cartId = "c-333";
        String metaKey = "sess:" + sessionId + ":meta";
        String itemsKey = "cart:" + cartId + ":items";
        String itemKey  = "cart:" + cartId + ":item:p-1";
        String countIdx = "cart:idx:counts";

        // Resolve cartId and simulate existing membership/hash
        when(jedis.hget(metaKey, "cart_id")).thenReturn(cartId);
        when(jedis.sadd(itemsKey, "p-1")).thenReturn(0L);  // already a member
        when(jedis.hgetAll(itemKey)).thenReturn(Map.of(
                "name", "Mug",
                "short_desc", "Ceramic",
                "amount", "2",
                "total_price", "19.98"
        ));

        CartItem delta = CartItem.builder()
                .productId("p-1")
                .name("Mug")
                .shortDescription("Ceramic")
                .amount(1)
                .totalPrice(new BigDecimal("9.99"))
                .build();

        repository.add(sessionId, delta);

        // Interactions we expect
        verify(jedis).hget(metaKey, "cart_id");   // from cartIdForSession(...)
        verify(jedis).sadd(itemsKey, "p-1");
        verify(jedis).hgetAll(itemKey);

        // Capture updated hash and assert totals combined (2+1, 19.98+9.99)
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String,String>> cap = ArgumentCaptor.forClass((Class) Map.class);
        verify(jedis).hset(eq(itemKey), cap.capture());
        Map<String,String> written = cap.getValue();
        assertThat(written.get("cart_id")).isEqualTo(cartId);
        assertThat(written.get("product_id")).isEqualTo("p-1");
        assertThat(written.get("name")).isEqualTo("Mug");
        assertThat(written.get("short_desc")).isEqualTo("Ceramic");
        assertThat(written.get("amount")).isEqualTo("3");        // 2 + 1
        assertThat(written.get("total_price")).isEqualTo("29.97"); // 19.98 + 9.99

        // Ensure count index is NOT touched for existing membership
        verify(jedis, never()).zincrby(eq(countIdx), anyDouble(), anyString());
        verify(jedis, never()).zrem(eq(countIdx), anyString());

        verifyNoMoreInteractions(jedis);
    }

    // ---------- remove ----------

    @Test
    void remove_existing_product_deletes_hash_srem_and_decrements_count_index_by_sessionId_then_zrem_if_zero() {
        String sessionId = "sid-4";
        String cartId = "c-444";
        String metaKey  = "sess:" + sessionId + ":meta";
        String itemsKey = "cart:" + cartId + ":items";
        String itemKey  = "cart:" + cartId + ":item:p-7";
        String countIdx = "cart:idx:counts";

        when(jedis.hget(metaKey, "cart_id")).thenReturn(cartId);
        when(jedis.srem(itemsKey, "p-7")).thenReturn(1L);
        when(jedis.zincrby(countIdx, -1.0, sessionId)).thenReturn(0.0);

        repository.remove(sessionId, "p-7");

        verify(jedis).hget(metaKey, "cart_id");              // from cartIdForSession(...)
        verify(jedis).del(itemKey);
        verify(jedis).srem(itemsKey, "p-7");
        verify(jedis).zincrby(countIdx, -1.0, sessionId);
        verify(jedis).zrem(countIdx, sessionId);
        verifyNoMoreInteractions(jedis);
    }

    @Test
    void remove_missing_product_is_idempotent_and_does_not_touch_count_index() {
        String sessionId = "sid-5";
        String cartId = "c-555";
        String metaKey = "sess:" + sessionId + ":meta";
        String itemsKey = "cart:" + cartId + ":items";
        String itemKey = "cart:" + cartId + ":item:p-x";
        String countIdx = "cart:idx:counts";

        when(jedis.hget(metaKey, "cart_id")).thenReturn(cartId);
        when(jedis.srem(itemsKey, "p-x")).thenReturn(0L);

        repository.remove(sessionId, "p-x");

        // Expected interactions
        verify(jedis).hget(metaKey, "cart_id");
        verify(jedis).del(itemKey);
        verify(jedis).srem(itemsKey, "p-x");

        // No count index updates when membership didn't change
        verify(jedis, never()).zincrby(eq(countIdx), anyDouble(), anyString());
        verify(jedis, never()).zrem(eq(countIdx), anyString());

        verifyNoMoreInteractions(jedis);
    }

    // ---------- search ----------

    @Test
    void searchByShortDescription_maps_documents_to_cart_items_and_scopes_by_cart_id() {
        String sessionId = "sid-6";
        String cartId = "c-666";
        String metaKey = "sess:" + sessionId + ":meta";

        when(jedis.hget(metaKey, "cart_id")).thenReturn(cartId);

        // Fake RediSearch result
        SearchResult result = mock(SearchResult.class);
        Document doc = mock(Document.class);
        Map<String, Object> fields = Map.of(
                "product_id", "p-777",
                "name", "Travel Bottle",
                "short_desc", "Vacuum insulated steel bottle",
                "amount", "1",
                "total_price", "24.99"
        );
        when(doc.getProperties()).thenReturn(fields.entrySet());
        when(result.getDocuments()).thenReturn(List.of(doc));
        when(jedis.ftSearch(eq("idx:cart_items"), any(Query.class))).thenReturn(result);

        var items = repository.searchByShortDescription(sessionId, "steel");

        assertThat(items).hasSize(1);
        CartItem only = items.getFirst();
        assertThat(only.getProductId()).isEqualTo("p-777");
        assertThat(only.getName()).isEqualTo("Travel Bottle");
        assertThat(only.getShortDescription()).isEqualTo("Vacuum insulated steel bottle");
        assertThat(only.getAmount()).isEqualTo(1);
        assertThat(only.getTotalPrice()).isEqualByComparingTo("24.99");

        // Verify interactions, including cartId resolution
        verify(jedis).hget(metaKey, "cart_id");
        verify(jedis).ftSearch(eq("idx:cart_items"), any(Query.class));
        verifyNoMoreInteractions(jedis);
    }

    @Test
    void searchByShortDescription_with_blank_query_falls_back_to_findBySession() {
        String sessionId = "sid-7";
        String cartId = "c-777";
        String metaKey = "sess:" + sessionId + ":meta";
        String itemsKey = "cart:" + cartId + ":items";
        String itemKey = "cart:" + cartId + ":item:p-1";

        // When query is blank, repo delegates to findBySession:
        when(jedis.hget(metaKey, "cart_id")).thenReturn(cartId);
        when(jedis.smembers(itemsKey)).thenReturn(Set.of("p-1"));
        when(jedis.hgetAll(itemKey)).thenReturn(Map.of(
                "name", "Mug",
                "short_desc", "Ceramic",
                "amount", "1",
                "total_price", "9.99"
        ));

        var items = repository.searchByShortDescription(sessionId, "   ");

        assertThat(items).hasSize(1);
        assertThat(items.getFirst().getName()).isEqualTo("Mug");

        // Verify the fallback interactions explicitly
        verify(jedis).hget(metaKey, "cart_id");
        verify(jedis).smembers(itemsKey);
        verify(jedis).hgetAll(itemKey);

        // No RediSearch call expected on blank query
        verifyNoMoreInteractions(jedis);
    }

    // ---------- report ----------

    @Test
    void sessionsWithItemCountGreaterThan_returns_sessions_above_threshold_from_count_index() {
        when(jedis.zrangeByScore(COUNT_ZSET, "(10", "+inf"))
                .thenReturn(List.of("sid-A", "sid-B"));

        var sessions = repository.sessionsWithItemCountGreaterThan(10);

        assertThat(sessions).containsExactly("sid-A", "sid-B");
        verify(jedis).zrangeByScore(COUNT_ZSET, "(10", "+inf");
        verifyNoMoreInteractions(jedis);
    }

    // ---------- restore ----------

    @Test
    void restoreFromPreviousSession_rebinds_current_session_cart_to_second_most_recent_if_present() {
        String username = "alice";
        String currentSid = "sid-now";
        String previousSid = "sid-prev";
        String prevCartId = "c-prev";

        String userZ = "sess:user:" + username;
        String prevMeta = "sess:" + previousSid + ":meta";
        String curMeta = "sess:" + currentSid + ":meta";

        // second most recent = (1,1)
        when(jedis.zrevrange(userZ, 1, 1)).thenReturn(List.of(previousSid));
        when(jedis.hget(prevMeta, "cart_id")).thenReturn(prevCartId);

        repository.restoreFromPreviousSession(username, currentSid);

        verify(jedis).zrevrange(userZ, 1, 1);
        verify(jedis).hget(prevMeta, "cart_id");
        verify(jedis).hset(eq(curMeta), eq(Map.of("cart_id", prevCartId)));
        verifyNoMoreInteractions(jedis);
    }

    @Test
    void restoreFromPreviousSession_no_second_session_is_noop() {
        String username = "bob";
        String currentSid = "sid-only";
        when(jedis.zrevrange("sess:user:" + username, 1, 1)).thenReturn(Collections.emptyList());

        repository.restoreFromPreviousSession(username, currentSid);

        verify(jedis).zrevrange("sess:user:" + username, 1, 1);
        verifyNoMoreInteractions(jedis);
    }
}