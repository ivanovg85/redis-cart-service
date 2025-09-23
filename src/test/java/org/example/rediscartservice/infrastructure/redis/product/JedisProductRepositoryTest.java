package org.example.rediscartservice.infrastructure.redis.product;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.rediscartservice.domain.model.product.Product;
import org.example.rediscartservice.domain.port.product.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.json.Path2;
import redis.clients.jedis.search.Document;
import redis.clients.jedis.search.Query;
import redis.clients.jedis.search.SearchResult;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JedisProductRepositoryTest {

    @Mock
    JedisPooled jedis;

    ObjectMapper objectMapper;
    ProductRepository productRepository;

    @BeforeEach
    void setup() {
        objectMapper = new ObjectMapper();
        productRepository = new JedisProductRepository(jedis, objectMapper);
    }

    @Test
    void save_writes_json_and_returns_product() throws Exception {
        Product product = product("id-123", "SKU-0001", "Black Mug", "Stoneware mug", new BigDecimal("12.99"));

        Product saved = productRepository.save(product);
        assertThat(saved).isEqualTo(product);

        ArgumentCaptor<String> keyCap = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Path2> pathCap = ArgumentCaptor.forClass(Path2.class);
        ArgumentCaptor<String> jsonCap = ArgumentCaptor.forClass(String.class);

        verify(jedis).jsonSet(keyCap.capture(), pathCap.capture(), jsonCap.capture());
        assertThat(keyCap.getValue()).isEqualTo("product:id-123");
        assertThat(pathCap.getValue()).isEqualTo(Path2.ROOT_PATH);

        Product roundTrip = objectMapper.readValue(jsonCap.getValue(), Product.class);
        assertThat(roundTrip).isEqualTo(product);
    }

    @Test
    void findById_returns_product_when_present() throws Exception {
        Product product = product("id-456", "SKU-0002", "Blue Bottle", "Aluminum bottle", new BigDecimal("19.50"));
        String productJson = objectMapper.writeValueAsString(product);
        when(jedis.jsonGet("product:id-456", Path2.ROOT_PATH)).thenReturn(productJson);

        Optional<Product> opt = productRepository.findById("id-456");

        assertThat(opt).isPresent();
        assertThat(opt.get()).isEqualTo(product);
    }

    @Test
    void findById_returns_empty_when_missing() {
        when(jedis.jsonGet("product:missing", Path2.ROOT_PATH)).thenReturn(null);

        Optional<Product> productOptional = productRepository.findById("missing");

        assertThat(productOptional).isEmpty();
    }

    @Test
    void deleteById_deletes_key() {
        productRepository.deleteById("id-789");
        verify(jedis).del("product:id-789");
    }

    @Test
    void searchByName_returns_products_from_redisjson() throws Exception {
        Product product1 = product("id-100", "SKU-0100", "Red Mug", "Stoneware", new BigDecimal("9.99"));
        Product product2 = product("id-101", "SKU-0101", "Red Bottle", "Aluminum", new BigDecimal("14.50"));

        // Prepare docs (only ids used by repo)
        Document document1 = mock(Document.class);
        when(document1.getId()).thenReturn("product:id-100");
        Document document2 = mock(Document.class);
        when(document2.getId()).thenReturn("product:id-101");

        SearchResult searchResult = mock(SearchResult.class);
        when(searchResult.getDocuments()).thenReturn(List.of(document1, document2));
        when(jedis.ftSearch(eq("idx:products"), any(Query.class))).thenReturn(searchResult);

        when(jedis.jsonGet("product:id-100", Path2.ROOT_PATH)).thenReturn(objectMapper.writeValueAsString(product1));
        when(jedis.jsonGet("product:id-101", Path2.ROOT_PATH)).thenReturn(objectMapper.writeValueAsString(product2));

        List<Product> result = productRepository.searchByName("Red");

        assertThat(result).containsExactly(product1, product2);
        verify(jedis).ftSearch(eq("idx:products"), any(Query.class));
    }

    @Test
    void searchByDescription_returns_products_from_redisjson() throws Exception {
        Product product1 = product("id-200", "SKU-0200", "Blue T-Shirt", "Cotton tee", new BigDecimal("12.00"));
        Product product2 = product("id-201", "SKU-0201", "Green Hoodie", "Cotton fleece", new BigDecimal("29.00"));

        Document document1 = mock(Document.class);
        when(document1.getId()).thenReturn("product:id-200");
        Document document2 = mock(Document.class);
        when(document2.getId()).thenReturn("product:id-201");

        SearchResult sr = mock(SearchResult.class);
        when(sr.getDocuments()).thenReturn(List.of(document1, document2));
        when(jedis.ftSearch(eq("idx:products"), any(Query.class))).thenReturn(sr);

        when(jedis.jsonGet("product:id-200", Path2.ROOT_PATH)).thenReturn(objectMapper.writeValueAsString(product1));
        when(jedis.jsonGet("product:id-201", Path2.ROOT_PATH)).thenReturn(objectMapper.writeValueAsString(product2));

        List<Product> result = productRepository.searchByDescription("Cotton");

        assertThat(result).containsExactly(product1, product2);
        verify(jedis).ftSearch(eq("idx:products"), any(Query.class));
    }

    // ---- helpers ----
    private static Product product(String id, String sku, String name, String desc, BigDecimal price) {
        return Product.builder()
                .id(id)
                .sku(sku)
                .name(name)
                .description(desc)
                .price(price)
                .build();
    }
}