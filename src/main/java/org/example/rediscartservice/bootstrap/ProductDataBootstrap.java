package org.example.rediscartservice.bootstrap;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.example.rediscartservice.application.product.ProductService;
import org.example.rediscartservice.domain.model.product.Product;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductDataBootstrap implements CommandLineRunner {

    private final ProductService products;

    @Value("classpath:data/products.csv")
    private Resource csvResource;

    @Override
    public void run(String... args) throws Exception {
        if (csvResource == null || !csvResource.exists()) {
            log.warn("products.csv not found on classpath:data/products.csv — no products imported.");
            return;
        }

        List<Product> list = new ArrayList<>();
        try (InputStream in = csvResource.getInputStream();
             Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8);
             CSVParser parser = CSVFormat.DEFAULT
                     .builder()
                     .setHeader()
                     .setSkipHeaderRecord(true)
                     .build()
                     .parse(reader)) {

            for (CSVRecord rec : parser) {
                var p = Product.builder()
                        .id(rec.get("id").trim())
                        .sku(rec.get("sku").trim())
                        .name(rec.get("name").trim())
                        .description(rec.get("description").trim())
                        .price(new BigDecimal(rec.get("price").trim()))
                        .build();
                list.add(p);
            }
        }

        int ok = 0;
        for (Product p : list) {
            try {
                products.create(p);
                ok++;
            } catch (Exception e) {
                log.warn("Failed to import {} - {}", p.getSku(), e.getMessage());
            }
        }
        log.info("Imported {}/{} products from CSV.", ok, list.size());
    }
}
