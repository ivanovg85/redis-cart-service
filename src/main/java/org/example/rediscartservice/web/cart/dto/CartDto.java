package org.example.rediscartservice.web.cart.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartDto {

    @Schema(description = "Session identifier of the cart", example = "a7f6f7a2-0b8f-4a4d-9b1b-0f55e3c1c1c0")
    private String sessionId;

    @Schema(description = "Items contained in the cart")
    private List<CartItemDto> items;
}