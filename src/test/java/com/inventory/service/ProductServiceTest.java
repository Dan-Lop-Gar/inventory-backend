package com.inventory.service;

import com.inventory.dto.CreateProductRequest;
import com.inventory.dto.ProductResponse;
import com.inventory.entity.Category;
import com.inventory.entity.Product;
import com.inventory.entity.Supplier;
import com.inventory.exception.ProductNotFoundException;
import com.inventory.kafka.producer.StockAlertProducer;
import com.inventory.repository.CategoryRepository;
import com.inventory.repository.ProductRepository;
import com.inventory.repository.SupplierRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductService Tests")
class ProductServiceTest {

    @Mock
    ProductRepository productRepository;
    @Mock
    CategoryRepository categoryRepository;
    @Mock
    SupplierRepository supplierRepository;
    @Mock
    StockAlertProducer stockAlertProducer;

    ProductService productService;

    // Datos de prueba
    UUID productId = UUID.randomUUID();
    UUID categoryId = UUID.randomUUID();
    UUID supplierId = UUID.randomUUID();

    Category mockCategory;
    Supplier mockSupplier;
    Product mockProduct;

    @BeforeEach
    void setUp() {
        MeterRegistry registry = new SimpleMeterRegistry();
        productService = new ProductService(
                productRepository, categoryRepository,
                supplierRepository, stockAlertProducer, registry
        );

        mockCategory = Category.builder()
                .id(categoryId)
                .name("Electronics")
                .build();

        mockSupplier = Supplier.builder()
                .id(supplierId)
                .name("TechParts SA")
                .email("tech@test.com")
                .active(true)
                .build();

        mockProduct = Product.builder()
                .id(productId)
                .sku("ELEC-001")
                .name("Laptop Pro")
                .price(new BigDecimal("25000.00"))
                .stockCurrent(50)
                .stockMinimum(10)
                .active(true)
                .category(mockCategory)
                .supplier(mockSupplier)
                .build();
    }


    @Test
    @DisplayName("debe retornar producto cuando existe")
    void shouldReturnProductWhenExists() {
        when(productRepository.findActiveById(productId))
                .thenReturn(Optional.of(mockProduct));

        ProductResponse result = productService.findById(productId);

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(productId);
        assertThat(result.sku()).isEqualTo("ELEC-001");
        assertThat(result.name()).isEqualTo("Laptop Pro");
        assertThat(result.belowMinimumStock()).isFalse();

        verify(productRepository).findActiveById(productId);
    }

    @Test
    @DisplayName("debe lanzar ProductNotFoundException cuando no existe")
    void shouldThrowWhenNotFound() {
        when(productRepository.findActiveById(productId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.findById(productId))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessageContaining(productId.toString());
    }


    @Test
    @DisplayName("debe crear producto con stock normal")
    void shouldCreateProductWithNormalStock() {
        CreateProductRequest request = new CreateProductRequest(
                "NEW-001", "New Product", "Description",
                new BigDecimal("100.00"), 50, 10,
                categoryId, supplierId
        );

        when(categoryRepository.findById(categoryId))
                .thenReturn(Optional.of(mockCategory));
        when(supplierRepository.findById(supplierId))
                .thenReturn(Optional.of(mockSupplier));
        when(productRepository.save(any(Product.class)))
                .thenAnswer(inv -> {
                    Product p = inv.getArgument(0);
                    p = Product.builder()
                            .id(UUID.randomUUID())
                            .sku(p.getSku())
                            .name(p.getName())
                            .price(p.getPrice())
                            .stockCurrent(p.getStockCurrent())
                            .stockMinimum(p.getStockMinimum())
                            .active(true)
                            .category(mockCategory)
                            .supplier(mockSupplier)
                            .build();
                    return p;
                });

        ProductResponse result = productService.create(request);

        assertThat(result.sku()).isEqualTo("NEW-001");
        assertThat(result.active()).isTrue();
        assertThat(result.belowMinimumStock()).isFalse();

        // No debe enviar alerta — stock está bien
        verifyNoInteractions(stockAlertProducer);
    }

    @Test
    @DisplayName("debe enviar alerta cuando stock inicial es bajo")
    void shouldSendAlertWhenInitialStockIsLow() {
        CreateProductRequest request = new CreateProductRequest(
                "LOW-001", "Low Stock Product", null,
                new BigDecimal("100.00"), 5, 10,  // stock < mínimo
                categoryId, supplierId
        );

        when(categoryRepository.findById(categoryId))
                .thenReturn(Optional.of(mockCategory));
        when(supplierRepository.findById(supplierId))
                .thenReturn(Optional.of(mockSupplier));
        when(productRepository.save(any(Product.class)))
                .thenAnswer(inv -> {
                    Product p = inv.getArgument(0);
                    return Product.builder()
                            .id(UUID.randomUUID())
                            .sku(p.getSku())
                            .name(p.getName())
                            .price(p.getPrice())
                            .stockCurrent(5)
                            .stockMinimum(10)
                            .active(true)
                            .category(mockCategory)
                            .supplier(mockSupplier)
                            .build();
                });

        productService.create(request);

        // Debe enviar alerta — stock está por debajo del mínimo
        verify(stockAlertProducer).sendAlert(any(Product.class));
    }

    @Test
    @DisplayName("debe lanzar excepción cuando categoría no existe")
    void shouldThrowWhenCategoryNotFound() {
        CreateProductRequest request = new CreateProductRequest(
                "SKU-001", "Product", null,
                BigDecimal.TEN, 50, 10,
                UUID.randomUUID(), supplierId
        );

        when(categoryRepository.findById(any()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.create(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Categoría no encontrada");
    }

}