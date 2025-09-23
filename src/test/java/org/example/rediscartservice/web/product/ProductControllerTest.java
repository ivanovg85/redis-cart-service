package org.example.rediscartservice.web.product;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.rediscartservice.application.product.ProductService;
import org.example.rediscartservice.domain.model.product.Product;
import org.example.rediscartservice.web.product.dto.ProductDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ProductControllerTest {

    @Mock
    ProductService productService;

    MockMvc mvc;
    ObjectMapper objectMapper;

    @BeforeEach
    void setup() {
        objectMapper = new ObjectMapper();
        // Build standalone MockMvc with JSON converter + Bean Validation
        mvc = MockMvcBuilders
                .standaloneSetup(new ProductController(productService))
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .setValidator(new LocalValidatorFactoryBean())
                .build();
    }

    // ---------- CREATE ----------

    @Test
    void create_returns_201_with_generated_id_and_location() throws Exception {
        when(productService.create(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        var dto = ProductDto.builder()
                .sku("SKU-1000")
                .name("Admin Created")
                .description("Desc")
                .price(new BigDecimal("19.99"))
                .build();

        var result = mvc.perform(post("/api/products")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.sku").value("SKU-1000"))
                .andExpect(jsonPath("$.name").value("Admin Created"))
                .andReturn();

        var node = objectMapper.readTree(result.getResponse().getContentAsString());
        String id = node.get("id").asText();
        assertThat(isUuid(id)).isTrue();
        assertThat(result.getResponse().getHeader("Location")).isEqualTo("/api/products/" + id);

        verify(productService).create(argThat(p ->
                id.equals(p.getId()) &&
                        "SKU-1000".equals(p.getSku()) &&
                        "Admin Created".equals(p.getName())
        ));
    }

    @Test
    void create_returns_400_on_validation_error() throws Exception {
        // invalid: blank sku & name, >2 fraction digits in price
        var bad = ProductDto.builder()
                .sku(" ")
                .name("")
                .description("x")
                .price(new BigDecimal("12.999"))
                .build();

        mvc.perform(post("/api/products")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bad)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(productService);
    }

    // ---------- UPDATE ----------

    @Test
    void update_returns_200_and_uses_id_from_path() throws Exception {
        String pathId = UUID.randomUUID().toString();
        when(productService.update(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        var dto = ProductDto.builder()
                .sku("SKU-2000")
                .name("Updated")
                .description("New desc")
                .price(new BigDecimal("29.99"))
                .build();

        mvc.perform(put("/api/products/{id}", pathId)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(pathId))
                .andExpect(jsonPath("$.name").value("Updated"));

        verify(productService).update(argThat(p ->
                pathId.equals(p.getId()) &&
                        "SKU-2000".equals(p.getSku()) &&
                        "Updated".equals(p.getName())
        ));
    }

    // ---------- GET ----------

    @Test
    void get_returns_200_with_body() throws Exception {
        String id = UUID.randomUUID().toString();
        var prod = Product.builder()
                .id(id).sku("SKU-9").name("Name").description("D").price(new BigDecimal("9.99")).build();

        when(productService.get(id)).thenReturn(prod);

        mvc.perform(get("/api/products/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.sku").value("SKU-9"))
                .andExpect(jsonPath("$.name").value("Name"));
    }

    // ---------- SEARCH ----------

    @Test
    void searchByName_returns_list() throws Exception {
        var p1 = Product.builder().id(UUID.randomUUID().toString()).sku("S1").name("Mug").price(new BigDecimal("10.00")).build();
        var p2 = Product.builder().id(UUID.randomUUID().toString()).sku("S2").name("Bottle").price(new BigDecimal("12.00")).build();
        when(productService.searchByName("Mug")).thenReturn(List.of(p1, p2));

        mvc.perform(get("/api/products/search/name").param("q", "Mug"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(p1.getId()))
                .andExpect(jsonPath("$[1].id").value(p2.getId()));
    }

    @Test
    void searchByDescription_returns_list() throws Exception {
        var p = Product.builder().id(UUID.randomUUID().toString()).sku("S3").name("Cap").price(new BigDecimal("7.00")).build();
        when(productService.searchByDescription("cotton")).thenReturn(List.of(p));

        mvc.perform(get("/api/products/search/description").param("q", "cotton"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(p.getId()));
    }

    // ---------- DELETE ----------

    @Test
    void delete_returns_204() throws Exception {
        String id = UUID.randomUUID().toString();

        mvc.perform(delete("/api/products/{id}", id))
                .andExpect(status().isNoContent());

        verify(productService).delete(id);
    }

    // ---------- helpers ----------

    private static boolean isUuid(String s) {
        try { UUID.fromString(s); return true; } catch (Exception e) { return false; }
    }
}