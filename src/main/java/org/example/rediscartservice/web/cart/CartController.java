package org.example.rediscartservice.web.cart;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.example.rediscartservice.application.cart.CartService;
import org.example.rediscartservice.domain.model.cart.CartItem;
import org.example.rediscartservice.web.cart.dto.AddCartItemRequest;
import org.example.rediscartservice.web.cart.dto.CartItemDto;
import org.example.rediscartservice.web.cart.dto.CartDto;
import org.example.rediscartservice.web.security.annotations.AdminOnly;
import org.example.rediscartservice.web.security.annotations.SessionTouch;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/cart")
@Tag(name = "Cart", description = "Shopping cart operations")
@SessionTouch
public class CartController {

    private final CartService cartService;

    @Operation(summary = "Get the current user's cart")
    @ApiResponse(responseCode = "200", description = "List of cart items")
    @GetMapping
    public ResponseEntity<List<CartItemDto>> getCart(HttpSession httpSession) {
        String sessionId = httpSession.getId();
        List<CartItem> cartItems = cartService.findBySession(sessionId);
        return ResponseEntity.ok(cartItems.stream().map(this::toDto).toList());
    }

    @Operation(summary = "Add a product to the cart (increments if already present)")
    @ApiResponse(responseCode = "200", description = "Updated cart items")
    @PostMapping("/items")
    public ResponseEntity<List<CartItemDto>> addProduct(
            HttpSession httpSession,
            @Valid @RequestBody AddCartItemRequest requestBody
    ) {
        String sessionId = httpSession.getId();
        List<CartItem> updatedCartItems =
                cartService.addProduct(sessionId, requestBody.getProductId(), requestBody.getAmount());
        return ResponseEntity.ok(updatedCartItems.stream().map(this::toDto).toList());
    }

    @Operation(summary = "Remove a product from the cart (removes the whole amount)")
    @ApiResponse(responseCode = "200", description = "Updated cart items after removal")
    @DeleteMapping("/items/{productId}")
    public ResponseEntity<List<CartItemDto>> removeProduct(
            HttpSession httpSession,
            @PathVariable @NotBlank String productId
    ) {
        String sessionId = httpSession.getId();
        List<CartItem> updatedCartItems = cartService.removeProduct(sessionId, productId);
        return ResponseEntity.ok(updatedCartItems.stream().map(this::toDto).toList());
    }

    @Operation(summary = "Search items in the current cart by short description (case-insensitive)")
    @ApiResponse(responseCode = "200", description = "Matching cart items")
    @GetMapping("/items/search")
    public ResponseEntity<List<CartItemDto>> searchItems(
            HttpSession httpSession,
            @RequestParam("q") @NotBlank String query
    ) {
        String sessionId = httpSession.getId();
        List<CartItem> matches = cartService.searchCart(sessionId, query);
        return ResponseEntity.ok(matches.stream().map(this::toDto).toList());
    }

    @Operation(
            summary = "Restore cart from last active session",
            description = "Loads the cart associated with the user's last active session into the current session. " +
                    "Current cart contents (if any) will be replaced."
    )
    @ApiResponse(responseCode = "200", description = "Updated cart items after restoration")
    @PostMapping("/restore")
    public ResponseEntity<List<CartItemDto>> restoreCart(HttpSession httpSession, Authentication authentication) {
        String currentSessionId = httpSession.getId();
        String username = authentication.getName();

        var restoredItems = cartService.restoreLastCart(username, currentSessionId);

        var dtos = restoredItems.stream().map(this::toDto).toList();
        return ResponseEntity.ok(dtos);
    }

    @Operation(
            summary = "Admin: report carts containing more than N items",
            description = "Returns all shopping carts with item count strictly greater than the threshold (default 10)."
    )
    @ApiResponse(responseCode = "200", description = "List of carts > threshold")
    @AdminOnly
    @GetMapping("/report")
    public ResponseEntity<List<CartDto>> reportCarts(
            @RequestParam(name = "threshold", defaultValue = "10") @Min(1) int threshold
    ) {
        // 1) fetch session ids with more than `threshold` items
        List<String> sessionIds = cartService.findSessionsWithMoreThanItems(threshold);

        // 2) load each cart and map to DTO
        List<CartDto> report = sessionIds.stream()
                .map(sessionId -> {
                    var cartItems = cartService.findBySession(sessionId);
                    var itemDtos = cartItems.stream().map(this::toDto).toList();
                    return CartDto.builder()
                            .sessionId(sessionId)
                            .items(itemDtos)
                            .build();
                })
                .toList();

        return ResponseEntity.ok(report);
    }

    // ---- mapping helpers ----
    private CartItemDto toDto(CartItem cartItem) {
        return CartItemDto.builder()
                .productId(cartItem.getProductId())
                .name(cartItem.getName())
                .shortDescription(cartItem.getShortDescription())
                .amount(cartItem.getAmount())
                .totalPrice(cartItem.getTotalPrice())
                .build();
    }
}