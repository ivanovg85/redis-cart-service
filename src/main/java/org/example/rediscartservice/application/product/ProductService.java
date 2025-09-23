package org.example.rediscartservice.application.product;

import lombok.RequiredArgsConstructor;
import org.example.rediscartservice.domain.model.product.Product;
import org.example.rediscartservice.domain.port.product.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository products;

    public Product create(Product product) {
        return products.save(product);
    }

    public Product update(Product product) {
        String id = product.getId();
        products.findById(id).orElseThrow(() -> new NoSuchElementException("Product not found: " + id));
        return products.save(product);
    }

    public Product get(String id) {
        return products.findById(id).orElseThrow(() -> new NoSuchElementException("Product not found: " + id));
    }

    public void delete(String id) {
        products.deleteById(id);
    }

    public List<Product> searchByName(String query) {
        return products.searchByName(query);
    }

    public List<Product> searchByDescription(String query) {
        return products.searchByDescription(query);
    }

    public List<Product> listAll(int page, int size) {
        int p = Math.max(0, page);
        int s = Math.max(1, size);
        int offset = p * s;
        return products.findAll(offset, s);
    }
}