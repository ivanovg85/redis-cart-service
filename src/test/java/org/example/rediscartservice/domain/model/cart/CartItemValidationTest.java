package org.example.rediscartservice.domain.model.cart;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.ConstraintViolation;
import org.junit.jupiter.api.*;

import java.math.BigDecimal;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class CartItemValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDownValidator() {
        factory.close();
    }

    @Test
    void validCartItem_passesValidation() {
        CartItem item = CartItem.builder()
                .productId("p-1001")
                .name("Swagger Mug")
                .shortDescription("Stoneware mug")
                .amount(2)
                .totalPrice(new BigDecimal("25.98"))
                .build();

        Set<ConstraintViolation<CartItem>> violations = validator.validate(item);

        assertThat(violations).isEmpty();
    }

    @Test
    void blankProductId_failsValidation() {
        CartItem item = CartItem.builder()
                .productId("  ")
                .name("Swagger Mug")
                .shortDescription("Stoneware mug")
                .amount(1)
                .totalPrice(new BigDecimal("12.99"))
                .build();

        Set<ConstraintViolation<CartItem>> violations = validator.validate(item);

        assertThat(violations).extracting(ConstraintViolation::getPropertyPath)
                .anyMatch(path -> path.toString().equals("productId"));
    }

    @Test
    void zeroAmount_failsValidation() {
        CartItem item = CartItem.builder()
                .productId("p-1001")
                .name("Swagger Mug")
                .shortDescription("Stoneware mug")
                .amount(0)
                .totalPrice(new BigDecimal("0.00"))
                .build();

        Set<ConstraintViolation<CartItem>> violations = validator.validate(item);

        assertThat(violations).extracting(ConstraintViolation::getPropertyPath)
                .anyMatch(path -> path.toString().equals("amount"));
    }

    @Test
    void negativeTotalPrice_failsValidation() {
        CartItem item = CartItem.builder()
                .productId("p-1001")
                .name("Swagger Mug")
                .shortDescription("Stoneware mug")
                .amount(1)
                .totalPrice(new BigDecimal("-1.00"))
                .build();

        Set<ConstraintViolation<CartItem>> violations = validator.validate(item);

        assertThat(violations).extracting(ConstraintViolation::getPropertyPath)
                .anyMatch(path -> path.toString().equals("totalPrice"));
    }

    @Test
    void tooLongShortDescription_failsValidation() {
        String longDesc = "x".repeat(200);
        CartItem item = CartItem.builder()
                .productId("p-1001")
                .name("Swagger Mug")
                .shortDescription(longDesc)
                .amount(1)
                .totalPrice(new BigDecimal("12.99"))
                .build();

        Set<ConstraintViolation<CartItem>> violations = validator.validate(item);

        assertThat(violations).extracting(ConstraintViolation::getPropertyPath)
                .anyMatch(path -> path.toString().equals("shortDescription"));
    }
}