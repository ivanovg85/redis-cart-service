package org.example.rediscartservice.domain.model.product;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

class ProductValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setupValidator() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void closeFactory() {
        if (factory != null) factory.close();
    }

    @Test
    void valid_product_passes_validation() {
        var product = Product.builder()
                .id(UUID.randomUUID().toString())
                .sku("SKU-001")
                .name("Coffee Mug")
                .description("Stoneware 350ml")
                .price(new BigDecimal("12.99"))
                .build();

        Set<ConstraintViolation<Product>> violations = validator.validate(product);
        assertThat(violations).isEmpty();
    }

    @Test
    void negative_price_or_too_many_decimals_fail_validation() {
        var negative = Product.builder()
                .id("id-1")
                .sku("S1")
                .name("Name")
                .price(new BigDecimal("-0.01"))
                .build();

        var tooManyDecimals = Product.builder()
                .id("id-2")
                .sku("S2")
                .name("Name")
                .price(new BigDecimal("1.999"))
                .build();

        assertThat(validator.validate(negative)).isNotEmpty();
        assertThat(validator.validate(tooManyDecimals)).isNotEmpty();
    }

    @Test
    void blank_or_null_core_fields_fail_validation() {
        var missingCore = Product.builder()
                .id("")          // @NotBlank
                .sku(" ")        // @NotBlank
                .name(null)      // @NotBlank
                .price(new BigDecimal("1.00"))
                .build();

        assertThat(validator.validate(missingCore)).isNotEmpty();
    }
}