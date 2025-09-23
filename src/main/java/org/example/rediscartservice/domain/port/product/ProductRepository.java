package org.example.rediscartservice.domain.port.product;

import org.example.rediscartservice.domain.model.product.Product;

import java.util.List;
import java.util.Optional;

public interface ProductRepository {
    Product save(Product product);
    Optional<Product> findById(String id);
    void deleteById(String id);

    List<Product> searchByName(String textQuery);
    List<Product> searchByDescription(String textQuery);

    List<Product> findAll(int offset, int s);
}
