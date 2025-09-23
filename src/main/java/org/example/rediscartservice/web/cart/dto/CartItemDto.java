package org.example.rediscartservice.web.cart.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartItemDto {

    @Schema(description = "Id of the product", example = "a7f6f7a2-0b8f-4a4d-9b1b-0f55e3c1c1c0")
    private String productId;

    @Schema(description = "Name of the product", example = "Wireless Mouse")
    private String name;

    @Schema(description = "Shortened description of the product", example = "Ergonomic wireless mouse")
    private String shortDescription;

    @Schema(description = "Quantity of this product in the cart", example = "2")
    private int amount;

    @Schema(description = "Total price for this product (amount × unit price)", example = "59.98")
    private BigDecimal totalPrice;
}