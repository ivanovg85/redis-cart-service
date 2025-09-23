package org.example.rediscartservice.web.product;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.rediscartservice.application.product.ProductService;
import org.example.rediscartservice.domain.model.product.Product;
import org.example.rediscartservice.web.product.dto.ProductDto;
import org.example.rediscartservice.web.product.dto.ProductResponse;
import org.example.rediscartservice.web.security.annotations.AdminOnly;
import org.example.rediscartservice.web.security.annotations.Authenticated;
import org.example.rediscartservice.web.security.annotations.SessionTouch;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@Tag(name = "Products")
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@SessionTouch
public class ProductController {

    private final ProductService productService;

    @Operation(summary = "Get a product by id")
    @Authenticated
    @GetMapping("/{id}")
    public ProductResponse get(@PathVariable String id) {
        return toResponse(productService.get(id));
    }

    @Operation(summary = "Search products by name")
    @Authenticated
    @GetMapping("/search/name")
    public List<ProductResponse> searchByName(@RequestParam String q) {
        return productService.searchByName(q).stream().map(this::toResponse).toList();
    }

    @Operation(summary = "Search products by description")
    @Authenticated
    @GetMapping("/search/description")
    public List<ProductResponse> searchByDescription(@RequestParam String q) {
        return productService.searchByDescription(q).stream().map(this::toResponse).toList();
    }

    @Operation(summary = "Create a new product (ADMIN)")
    @AdminOnly
    @PostMapping
    public ResponseEntity<ProductResponse> create(@RequestBody @Valid ProductDto dto) {
        String id = UUID.randomUUID().toString();
        Product toCreate = toDomain(id, dto);
        Product saved = productService.create(toCreate);
        ProductResponse body = toResponse(saved);
        return ResponseEntity
                .created(URI.create("/api/products/" + saved.getId()))
                .body(body);
    }

    @Operation(summary = "Update a product by id (ADMIN)")
    @AdminOnly
    @PutMapping("/{id}")
    public ProductResponse update(@PathVariable String id, @RequestBody @Valid ProductDto dto) {
        Product toUpdate = toDomain(id, dto);
        Product saved = productService.update(toUpdate);
        return toResponse(saved);
    }

    @Operation(summary = "Delete a product by id (ADMIN)")
    @AdminOnly
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id) {
        productService.delete(id);
    }

    @Operation(summary = "List all products (paged)")
    @GetMapping
    @Authenticated
    public ResponseEntity<List<ProductResponse>> listAll(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "50") int size
    ) {
        List<Product> all = productService.listAll(page, size);
        List<ProductResponse> body = all.stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(body);
    }

    // ---------- mapping helpers ----------

    private Product toDomain(String id, ProductDto dto) {
        return Product.builder()
                .id(id)
                .sku(dto.getSku())
                .name(dto.getName())
                .description(dto.getDescription())
                .price(dto.getPrice())
                .build();
    }

    private ProductResponse toResponse(Product p) {
        return ProductResponse.builder()
                .id(p.getId())
                .sku(p.getSku())
                .name(p.getName())
                .description(p.getDescription())
                .price(p.getPrice())
                .build();
    }
}