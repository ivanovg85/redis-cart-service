package org.example.rediscartservice.application.product;

import org.example.rediscartservice.domain.model.product.Product;
import org.example.rediscartservice.domain.port.product.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    ProductRepository repo;

    ProductService service;

    @BeforeEach
    void setUp() {
        service = new ProductService(repo);
    }

    // ---- create ----
    @Test
    void create_saves_product_as_is() {
        String id = UUID.randomUUID().toString();
        Product input = product(id, "SKU-001", "Black Mug", "Stoneware mug", "12.99");

        when(repo.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        Product saved = service.create(input);

        assertThat(saved).isEqualTo(input);
        ArgumentCaptor<Product> cap = ArgumentCaptor.forClass(Product.class);
        verify(repo).save(cap.capture());
        assertThat(cap.getValue()).isEqualTo(input);
    }

    // ---- update ----
    @Test
    void update_saves_when_existing() {
        String id = UUID.randomUUID().toString();
        Product patch = product(id, "SKU-001", "Mug v2", "New desc", "15.00");

        when(repo.findById(id)).thenReturn(Optional.of(existing(id)));
        when(repo.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        Product updated = service.update(patch);

        assertThat(updated).isEqualTo(patch);
        verify(repo).findById(id);
        verify(repo).save(patch);
    }

    @Test
    void update_throws_when_missing() {
        String id = "missing";
        Product patch = product(id, "SKU-404", "X", "Y", "1.00");
        when(repo.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(patch))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining(id);

        verify(repo, never()).save(any());
    }

    // ---- get ----
    @Test
    void get_returns_when_present() {
        String id = "p-1";
        Product stored = existing(id);
        when(repo.findById(id)).thenReturn(Optional.of(stored));

        Product got = service.get(id);

        assertThat(got).isEqualTo(stored);
    }

    @Test
    void get_throws_when_missing() {
        when(repo.findById("nope")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.get("nope"))
                .isInstanceOf(NoSuchElementException.class);
    }

    // ---- delete ----
    @Test
    void delete_invokes_repository() {
        service.delete("p-2");
        verify(repo).deleteById("p-2");
    }

    // ---- search ----
    @Test
    void searchByName_passes_query_and_returns_repo_result() {
        String incoming = "  Mug  "; // keep spaces on purpose
        List<Product> expected = List.of(existing("a"), existing("b"));
        when(repo.searchByName(incoming)).thenReturn(expected);

        List<Product> result = service.searchByName(incoming);

        // verify delegation
        verify(repo).searchByName(incoming);

        // verify the returned value (exact same instance + contents)
        assertThat(result).isSameAs(expected);
        assertThat(result).containsExactlyElementsOf(expected);
    }

    @Test
    void searchByDescription_passes_query_and_returns_repo_result() {
        String incoming = " stoneware ";
        Product only = existing("c");
        List<Product> expected = List.of(only);
        when(repo.searchByDescription(incoming)).thenReturn(expected);

        List<Product> result = service.searchByDescription(incoming);

        // verify delegation
        verify(repo).searchByDescription(incoming);

        // verify the returned value (exact same instance + contents)
        assertThat(result).isSameAs(expected);
        assertThat(result).containsExactly(only);
    }

    // ---- helpers ----
    private static Product product(String id, String sku, String name, String desc, String price) {
        return Product.builder()
                .id(id)
                .sku(sku)
                .name(name)
                .description(desc)
                .price(new java.math.BigDecimal(price))
                .build();
    }

    private static Product existing(String id) {
        return product(id, "SKU-" + id, "Name " + id, "Desc " + id, "9.99");
    }
}