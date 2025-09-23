package org.example.rediscartservice.application.cart;

import org.example.rediscartservice.application.product.ProductService;
import org.example.rediscartservice.domain.model.cart.CartItem;
import org.example.rediscartservice.domain.model.product.Product;
import org.example.rediscartservice.domain.port.cart.CartRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.*;

class CartServiceTest {

    private CartRepository cartRepository;
    private CartService cartService;
    private ProductService productService;

    @BeforeEach
    void setUp() {
        cartRepository = mock(CartRepository.class);
        productService = mock(ProductService.class);
        cartService = new CartService(cartRepository, productService);
    }

    @Test
    void findBySession_delegates_to_repository_and_returns_cart_items() {
        String sessionId = "sess-123";
        CartItem cartItem = CartItem.builder()
                .productId("p1")
                .name("Sample Mug")
                .shortDescription("Ceramic")
                .amount(2)
                .totalPrice(new BigDecimal("19.98"))
                .build();

        when(cartRepository.findBySession(sessionId)).thenReturn(List.of(cartItem));

        List<CartItem> result = cartService.findBySession(sessionId);

        assertThat(result).containsExactly(cartItem);
        verify(cartRepository).findBySession(sessionId);
        verifyNoMoreInteractions(cartRepository);
    }

    @Test
    void findBySession_with_null_session_id_throws_NullPointerException_and_repository_not_called() {
        try {
            cartService.findBySession(null);
        } catch (NullPointerException expected) {
            // expected — defensive guard
        }
        verifyNoInteractions(cartRepository);
    }

    @Test
    void addProduct_delegates_to_repository_then_returns_updated_cart() {
        String sessionId = "sess-1";
        String productId = "p-123";
        int amount = 2;

        Product product = Product.builder()
                .id(productId).sku("SKU-1").name("Sample Mug")
                .description("Ceramic mug").price(new BigDecimal("9.99"))
                .build();

        when(productService.get(productId)).thenReturn(product);

        CartItem expected = CartItem.builder()
                .productId(productId).name("Sample Mug")
                .shortDescription("Ceramic mug").amount(2)
                .totalPrice(new BigDecimal("19.98")).build();

        when(cartRepository.findBySession(sessionId)).thenReturn(List.of(expected));

        List<CartItem> result = cartService.addProduct(sessionId, productId, amount);

        assertThat(result).containsExactly(expected);

        // verify we constructed the snapshot correctly and called add(...)
        verify(cartRepository).add(eq(sessionId), argThat(ci ->
                ci.getProductId().equals(productId) &&
                        ci.getName().equals("Sample Mug") &&
                        ci.getShortDescription().equals("Ceramic mug") &&
                        ci.getAmount() == 2 &&
                        ci.getTotalPrice().compareTo(new BigDecimal("19.98")) == 0
        ));
        verify(cartRepository).findBySession(sessionId);
        verify(productService).get(productId);
        verifyNoMoreInteractions(cartRepository, productService);
    }

    @Test
    void addProduct_with_null_sessionId_throws_NullPointerException() {
        assertThatThrownBy(() -> cartService.addProduct(null, "p1", 1))
                .isInstanceOf(NullPointerException.class);
        verifyNoInteractions(cartRepository, productService);
    }

    @Test
    void addProduct_with_null_productId_throws_NullPointerException() {
        assertThatThrownBy(() -> cartService.addProduct("sess", null, 1))
                .isInstanceOf(NullPointerException.class);
        verifyNoInteractions(cartRepository, productService);
    }

    @Test
    void addProduct_with_non_positive_amount_throws_IllegalArgumentException() {
        assertThatThrownBy(() -> cartService.addProduct("sess", "p1", 0))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(cartRepository, productService);
    }

    @Test
    void removeProduct_with_valid_args_deletes_line_and_returns_updated_cart() {
        String sessionId = "sess-42";
        String productId = "p-123";

        var remaining = CartItem.builder()
                .productId("p-999")
                .name("Notebook")
                .shortDescription("A5 dotted")
                .amount(3)
                .totalPrice(new BigDecimal("14.97"))
                .build();

        // After removal, service asks repository for the updated cart
        when(cartRepository.findBySession(sessionId)).thenReturn(List.of(remaining));

        List<CartItem> result = cartService.removeProduct(sessionId, productId);

        assertThat(result).containsExactly(remaining);
        verify(cartRepository).remove(sessionId, productId);
        verify(cartRepository).findBySession(sessionId);
        verifyNoMoreInteractions(cartRepository, productService);
    }

    @Test
    void removeProduct_with_null_sessionId_throws_NullPointerException_and_repository_not_called() {
        assertThatThrownBy(() -> cartService.removeProduct(null, "p1"))
                .isInstanceOf(NullPointerException.class);
        verifyNoInteractions(cartRepository, productService);
    }

    @Test
    void removeProduct_with_null_productId_throws_NullPointerException_and_repository_not_called() {
        assertThatThrownBy(() -> cartService.removeProduct("sess", null))
                .isInstanceOf(NullPointerException.class);
        verifyNoInteractions(cartRepository, productService);
    }

    @Test
    void searchCart_with_blank_query_returns_full_cart() {
        String sessionId = "sess-1";
        var item = CartItem.builder()
                .productId("p1")
                .name("Sample Mug")
                .shortDescription("Ceramic mug")
                .amount(2)
                .totalPrice(new BigDecimal("19.98"))
                .build();

        when(cartRepository.findBySession(sessionId)).thenReturn(List.of(item));

        var result1 = cartService.searchCart(sessionId, "");
        var result2 = cartService.searchCart(sessionId, "   ");
        var result3 = cartService.searchCart(sessionId, null);

        assertThat(result1).containsExactly(item);
        assertThat(result2).containsExactly(item);
        assertThat(result3).containsExactly(item);

        verify(cartRepository, times(3)).findBySession(sessionId);
        verifyNoMoreInteractions(cartRepository, productService);
    }

    @Test
    void searchCart_with_non_blank_query_delegates_to_repository_search() {
        String sessionId = "sess-2";
        String query = "steel";
        var item = CartItem.builder()
                .productId("p2")
                .name("Travel Bottle")
                .shortDescription("Vacuum insulated steel bottle")
                .amount(1)
                .totalPrice(new BigDecimal("24.99"))
                .build();

        when(cartRepository.searchByShortDescription(eq(sessionId), eq("steel"))).thenReturn(List.of(item));

        var result = cartService.searchCart(sessionId, "  steel  ");

        assertThat(result).containsExactly(item);
        verify(cartRepository).searchByShortDescription(sessionId, "steel"); // trimmed
        verifyNoMoreInteractions(cartRepository, productService);
    }

    @Test
    void searchCart_with_null_session_id_throws_NullPointerException_and_repository_not_called() {
        assertThatThrownBy(() -> cartService.searchCart(null, "anything"))
                .isInstanceOf(NullPointerException.class);
        verifyNoInteractions(cartRepository, productService);
    }

    @Test
    void findSessionsWithMoreThanItems_delegates_to_repository() {
        when(cartRepository.sessionsWithItemCountGreaterThan(10))
                .thenReturn(List.of("sid-A", "sid-B"));

        List<String> result = cartService.findSessionsWithMoreThanItems(10);

        assertThat(result).containsExactly("sid-A", "sid-B");
        verify(cartRepository).sessionsWithItemCountGreaterThan(10);
        verifyNoMoreInteractions(cartRepository, productService);
    }

    @Test
    void findSessionsWithMoreThanItems_with_negative_threshold_throws() {
        assertThatThrownBy(() -> cartService.findSessionsWithMoreThanItems(-1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("threshold must be >= 0");
        verifyNoInteractions(cartRepository, productService);
    }
}