package org.example.rediscartservice.application.cart;

import lombok.RequiredArgsConstructor;
import org.example.rediscartservice.application.product.ProductService;
import org.example.rediscartservice.domain.model.cart.CartItem;
import org.example.rediscartservice.domain.model.product.Product;
import org.example.rediscartservice.domain.port.cart.CartRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class CartService {

    private static final int SHORT_DESC_LIMIT = 160;

    private final CartRepository cartRepository;
    private final ProductService productService;

    /**
     * Return all cart items for the given session.
     */
    public List<CartItem> findBySession(String sessionId) {
        Objects.requireNonNull(sessionId, "sessionId must not be null");
        return cartRepository.findBySession(sessionId);
    }

    /**
     * Add a product to the cart (or increment if already present).
     * Builds a CartItem snapshot from the Product at the time of adding.
     */
    public List<CartItem> addProduct(String sessionId, String productId, int amount) {
        Objects.requireNonNull(sessionId, "sessionId must not be null");
        Objects.requireNonNull(productId, "productId must not be null");
        if (amount <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }

        Product product = productService.get(productId);

        BigDecimal unitPrice = product.getPrice() == null
                ? BigDecimal.ZERO
                : product.getPrice();
        BigDecimal totalPrice = unitPrice
                .multiply(BigDecimal.valueOf(amount))
                .setScale(2, RoundingMode.HALF_UP);

        CartItem snapshot = CartItem.builder()
                .productId(productId)
                .name(product.getName())
                .shortDescription(shorten(product.getDescription(), SHORT_DESC_LIMIT))
                .amount(amount)
                .totalPrice(totalPrice)
                .build();

        cartRepository.add(sessionId, snapshot);
        return cartRepository.findBySession(sessionId);
    }

    public List<CartItem> removeProduct(String sessionId, String productId) {
        Objects.requireNonNull(sessionId, "sessionId must not be null");
        Objects.requireNonNull(productId, "productId must not be null");
        cartRepository.remove(sessionId, productId);
        return cartRepository.findBySession(sessionId);
    }

    /**
     * Search items in the user's cart by short description (case-insensitive).
     * Empty/blank queries return the full cart.
     */
    public List<CartItem> searchCart(String sessionId, String query) {
        Objects.requireNonNull(sessionId, "sessionId must not be null");
        String trimmed = query == null ? "" : query.trim();
        if (trimmed.isEmpty()) {
            return cartRepository.findBySession(sessionId);
        }
        return cartRepository.searchByShortDescription(sessionId, trimmed);
    }

    /**
     * Restore the cart from the user's last active session into the current session.
     * If there is no previous session, or it equals the current session, this is a no-op.
     * Returns the updated cart for the current session.
     */
    public List<CartItem> restoreLastCart(String username, String currentSessionId) {
        Objects.requireNonNull(username, "username must not be null");
        Objects.requireNonNull(currentSessionId, "currentSessionId must not be null");

        cartRepository.restoreFromPreviousSession(username, currentSessionId);

        return cartRepository.findBySession(currentSessionId);
    }

    /**
     * Admin report helper: return session ids whose carts contain STRICTLY more than {@code threshold} items.
     */
    public List<String> findSessionsWithMoreThanItems(int threshold) {
        if (threshold < 0) {
            throw new IllegalArgumentException("threshold must be >= 0");
        }
        return cartRepository.sessionsWithItemCountGreaterThan(threshold);
    }

    // ---------- helpers ----------

    private static String shorten(String text, int limit) {
        if (text == null) return "";
        String trimmed = text.trim();
        if (trimmed.length() <= limit) return trimmed;
        // keep it readable, add ellipsis
        return trimmed.substring(0, Math.max(0, limit - 1)).trim() + "…";
    }
}