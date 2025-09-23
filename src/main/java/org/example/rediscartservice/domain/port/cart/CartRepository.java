package org.example.rediscartservice.domain.port.cart;

import org.example.rediscartservice.domain.model.cart.CartItem;

import java.util.List;

public interface CartRepository {

    /**
     * Return all cart items for the given session id.
     * If the cart is empty or the session has no items, returns an empty list.
     */
    List<CartItem> findBySession(String sessionId);

    /** Add (or increment if exists) a cart line. */
    void add(String sessionId, CartItem cartItem);

    /** Remove a cart line entirely (idempotent). */
    void remove(String sessionId, String productId);

    /** Case-insensitive search by short description for a single session. */
    List<CartItem> searchByShortDescription(String sessionId, String query);

    /** Restore cart for currentSessionId from the user's previous session (if any). */
    void restoreFromPreviousSession(String username, String currentSessionId);

    /** Return session ids whose carts contain STRICTLY more than the given item count. */
    List<String> sessionsWithItemCountGreaterThan(int threshold);
}