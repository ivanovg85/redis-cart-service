package org.example.rediscartservice.web.product.dto;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

@Value
@Builder
public class ProductResponse {
    String id;
    String sku;
    String name;
    String description;
    BigDecimal price;
}
