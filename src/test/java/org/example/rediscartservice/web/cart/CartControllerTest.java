package org.example.rediscartservice.web.cart;

import org.example.rediscartservice.application.cart.CartService;
import org.example.rediscartservice.domain.model.cart.CartItem;
import org.example.rediscartservice.web.SecurityTestConfig;
import org.example.rediscartservice.web.cart.dto.CartDto;
import org.example.rediscartservice.web.cart.dto.CartItemDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.annotation.Resource;

import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers = CartController.class)
@Import({SecurityTestConfig.class, CartControllerTestConfig.class})
class CartControllerTest {

    @Resource
    MockMvc mockMvc;

    @Resource
    CartService cartService;

    @Test
    @WithMockUser(username = "user", roles = "USER")
    void getCart_with_cart_items_returns_ok_and_maps_to_dtos() throws Exception {
        CartItem sampleCartItem = CartItem.builder()
                .productId("p-123")
                .name("Sample Mug")
                .shortDescription("Ceramic mug")
                .amount(2)
                .totalPrice(new BigDecimal("19.98"))
                .build();

        when(cartService.findBySession(anyString())).thenReturn(List.of(sampleCartItem));

        mockMvc.perform(get("/api/cart"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name", is("Sample Mug")))
                .andExpect(jsonPath("$[0].shortDescription", is("Ceramic mug")))
                .andExpect(jsonPath("$[0].amount", is(2)))
                .andExpect(jsonPath("$[0].totalPrice", is(19.98)));
    }


    @Test
    @WithMockUser(username = "user", roles = "USER")
    void addProduct_with_valid_request_returns_ok_and_maps_to_dtos() throws Exception {
        CartItem updatedItem = CartItem.builder()
                .productId("p-123")
                .name("Sample Mug")
                .shortDescription("Ceramic mug")
                .amount(2)
                .totalPrice(new BigDecimal("19.98"))
                .build();
        when(cartService.addProduct(anyString(), anyString(), anyInt()))
                .thenReturn(List.of(updatedItem));

        // When / Then
        String requestJson = """
            {
              "productId": "p-123",
              "amount": 2
            }
            """;

        mockMvc.perform(post("/api/cart/items")
                        .contentType(APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name", is("Sample Mug")))
                .andExpect(jsonPath("$[0].shortDescription", is("Ceramic mug")))
                .andExpect(jsonPath("$[0].amount", is(2)))
                .andExpect(jsonPath("$[0].totalPrice", is(19.98)));
    }

    @Test
    @WithMockUser(username = "user", roles = "USER")
    void removeProduct_with_valid_product_id_returns_ok_and_updated_cart() throws Exception {
        // Given: after removing p-123, the cart contains one remaining item (p-999)
        CartItem remaining = CartItem.builder()
                .productId("p-999")
                .name("Notebook")
                .shortDescription("A5 dotted")
                .amount(3)
                .totalPrice(new BigDecimal("14.97"))
                .build();

        when(cartService.removeProduct(anyString(), anyString()))
                .thenReturn(List.of(remaining));

        // When / Then
        mockMvc.perform(delete("/api/cart/items/{productId}", "p-123")
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name", is("Notebook")))
                .andExpect(jsonPath("$[0].shortDescription", is("A5 dotted")))
                .andExpect(jsonPath("$[0].amount", is(3)))
                .andExpect(jsonPath("$[0].totalPrice", is(14.97)));
    }

    @Test
    @WithMockUser(username = "user", roles = "USER")
    void searchItems_with_query_returns_ok_and_maps_to_dtos() throws Exception {
        CartItem match = CartItem.builder()
                .productId("p-777")
                .name("Travel Bottle")
                .shortDescription("Vacuum insulated steel bottle")
                .amount(1)
                .totalPrice(new BigDecimal("24.99"))
                .build();

        when(cartService.searchCart(anyString(), anyString()))
                .thenReturn(List.of(match));

        mockMvc.perform(get("/api/cart/items/search")
                        .param("q", "steel"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name", is("Travel Bottle")))
                .andExpect(jsonPath("$[0].shortDescription", is("Vacuum insulated steel bottle")))
                .andExpect(jsonPath("$[0].amount", is(1)))
                .andExpect(jsonPath("$[0].totalPrice", is(24.99)));
    }

    @Test
    @WithMockUser(username = "user", roles = "USER")
    void restoreCart_returns_ok_and_updated_cart_for_current_session() throws Exception {
        CartItem restored = CartItem.builder()
                .productId("p-42")
                .name("Travel Bottle")
                .shortDescription("Vacuum insulated")
                .amount(1)
                .totalPrice(new BigDecimal("24.99"))
                .build();

        when(cartService.restoreLastCart(anyString(), anyString()))
                .thenReturn(List.of(restored));

        mockMvc.perform(post("/api/cart/restore"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name", is("Travel Bottle")))
                .andExpect(jsonPath("$[0].shortDescription", is("Vacuum insulated")))
                .andExpect(jsonPath("$[0].amount", is(1)))
                .andExpect(jsonPath("$[0].totalPrice", is(24.99)));
    }

    @Test
    void reportCarts_returns_list_of_carts_for_admin_user() throws Exception {
        CartItemDto item = new CartItemDto("p1", "Test", "Short", 2, new BigDecimal("19.98"));
        CartDto cart = new CartDto("session-123", List.of(item));
        CartItem cartItem = CartItem.builder()
                .productId("p1")
                .name("Test")
                .shortDescription("Short")
                .amount(2)
                .totalPrice(new BigDecimal("19.98"))
                .build();

        Mockito.when(cartService.findSessionsWithMoreThanItems(eq(10)))
                .thenReturn(List.of("session-123"));
        Mockito.when(cartService.findBySession("session-123"))
                .thenReturn(List.of(cartItem));

        mockMvc.perform(get("/api/cart/report?threshold=10")
                        .with(SecurityMockMvcRequestPostProcessors.httpBasic("admin", "admin123"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].sessionId").value("session-123"))
                .andExpect(jsonPath("$[0].items[0].name").value("Test"))
                .andExpect(jsonPath("$[0].items[0].amount").value(2))
                .andExpect(jsonPath("$[0].items[0].totalPrice").value(19.98));
    }

    @Test
    void reportCarts_requires_admin_authentication() throws Exception {
        mockMvc.perform(get("/api/cart/report?threshold=10")
                        .with(SecurityMockMvcRequestPostProcessors.httpBasic("user", "user123"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }
}