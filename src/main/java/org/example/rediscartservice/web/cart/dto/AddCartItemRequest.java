package org.example.rediscartservice.web.cart.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class AddCartItemRequest {

    @Schema(description = "Product identifier to add", example = "38c844a4-0ab0-4e0f-8d63-c7129bf97578", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    private String productId;

    @Schema(description = "Quantity to add (must be positive)", example = "2", requiredMode = Schema.RequiredMode.REQUIRED)
    @Positive
    private int amount;
}