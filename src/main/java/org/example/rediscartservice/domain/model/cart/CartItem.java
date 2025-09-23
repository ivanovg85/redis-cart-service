package org.example.rediscartservice.domain.model.cart;

import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

/**
 * A single product line in a user's cart.
 * - totalPrice is per SKU (amount × unit price at the time of adding).
 * - No cart-level aggregates here by design.
 */
@Value
@Builder(toBuilder = true)
public class CartItem {

    /** Internal product identifier (or SKU if you keyed by SKU). */
    @NotBlank
    String productId;

    /** Product display name. */
    @NotBlank
    String name;

    /** Shortened description used for cart display/search. */
    @Size(max = 160)
    String shortDescription;

    /** Quantity in the cart (must be >= 1). */
    @Min(1)
    int amount;

    /**
     * Per-SKU total (amount × unit price) captured at add time.
     * Two-decimal fixed scale, non-negative.
     */
    @NotNull
    @DecimalMin(value = "0.00", inclusive = true)
    @Digits(integer = 12, fraction = 2)
    BigDecimal totalPrice;
}