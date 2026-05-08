package com.inventory.service;

import com.inventory.config.HazelcastConfig;
import com.inventory.entity.Category;
import com.inventory.entity.Product;
import com.inventory.entity.Supplier;
import com.inventory.repository.CategoryRepository;
import com.inventory.repository.ProductRepository;
import com.inventory.repository.SupplierRepository;
import com.inventory.specification.ProductSpecification;
import com.inventory.dto.CreateProductRequest;
import com.inventory.dto.ProductFilterRequest;
import com.inventory.dto.UpdateProductRequest;
import com.inventory.dto.PagedResponse;
import com.inventory.dto.ProductResponse;
import com.inventory.dto.StockStatsResponse;
import com.inventory.exception.ProductNotFoundException;
import com.inventory.kafka.producer.StockAlertProducer;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final SupplierRepository supplierRepository;
    private final StockAlertProducer stockAlertProducer;
    private final Counter stockAlertCounter;
    private final Counter cacheHitCounter;

    public ProductService(
            ProductRepository productRepository,
            CategoryRepository categoryRepository,
            SupplierRepository supplierRepository,
            StockAlertProducer stockAlertProducer,
            MeterRegistry meterRegistry) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.supplierRepository = supplierRepository;
        this.stockAlertProducer = stockAlertProducer;
        this.stockAlertCounter = Counter.builder("inventory_stock_alerts_total")
            .description("Total de alertas de stock enviadas")
            .register(meterRegistry);
        this.cacheHitCounter = Counter.builder("inventory_cache_hits_total")
            .tag("cache", "products")
            .description("Total de cache hits en productos")
            .register(meterRegistry);
    }

    @Transactional(readOnly = true)
    public PagedResponse<ProductResponse> findAll(ProductFilterRequest filter) {
        Sort sort = buildSort(filter.sort());
        PageRequest pageable = PageRequest.of(filter.page(), filter.size(), sort);

        Specification<Product> spec = ProductSpecification.withFilters(
            filter.name(),
            filter.sku(),
            filter.categoryId(),
            filter.supplierId(),
            filter.minPrice(),
            filter.maxPrice(),
            filter.active(),
            filter.belowMinimumStock()
        );

        Page<ProductResponse> page = productRepository.findAll(spec, pageable)
            .map(ProductResponse::from);

        return PagedResponse.from(page);
    }

    @Cacheable(value = HazelcastConfig.CACHE_PRODUCTS, key = "#id")
    @Transactional(readOnly = true)
    public ProductResponse findById(UUID id) {
        cacheHitCounter.increment();
        log.debug("Buscando producto por ID: {}", id);

        return productRepository.findActiveById(id)
            .map(ProductResponse::from)
            .orElseThrow(() -> new ProductNotFoundException(id));
    }

    @CacheEvict(value = HazelcastConfig.CACHE_PRODUCTS, allEntries = true)
    @Transactional
    public ProductResponse create(CreateProductRequest request) {
        log.info("Creando producto SKU: {}", request.sku());

        Category category = categoryRepository.findById(request.categoryId())
            .orElseThrow(() -> new IllegalArgumentException(
                "Categoría no encontrada: " + request.categoryId()
            ));

        Supplier supplier = supplierRepository.findById(request.supplierId())
            .orElseThrow(() -> new IllegalArgumentException(
                "Proveedor no encontrado: " + request.supplierId()
            ));

        Product product = Product.builder()
            .sku(request.sku())
            .name(request.name())
            .description(request.description())
            .price(request.price())
            .stockCurrent(request.stockCurrent())
            .stockMinimum(request.stockMinimum())
            .active(true)
            .category(category)
            .supplier(supplier)
            .build();

        Product saved = productRepository.save(product);
        log.info("Producto creado: {} - {}", saved.getId(), saved.getSku());

        // Verifica si ya viene con stock bajo
        if (saved.isBelowMinimumStock()) {
            sendStockAlert(saved);
        }

        return ProductResponse.from(saved);
    }

    @CacheEvict(value = HazelcastConfig.CACHE_PRODUCTS, key = "#id")
    @Transactional
    public ProductResponse update(UUID id, UpdateProductRequest request) {
        log.info("Actualizando producto: {}", id);

        Product product = productRepository.findActiveById(id)
            .orElseThrow(() -> new ProductNotFoundException(id));

        Category category = categoryRepository.findById(request.categoryId())
            .orElseThrow(() -> new IllegalArgumentException(
                "Categoría no encontrada: " + request.categoryId()
            ));

        Supplier supplier = supplierRepository.findById(request.supplierId())
            .orElseThrow(() -> new IllegalArgumentException(
                "Proveedor no encontrado: " + request.supplierId()
            ));

        product.setName(request.name());
        product.setDescription(request.description());
        product.setPrice(request.price());
        product.setStockMinimum(request.stockMinimum());
        product.setActive(request.active());
        product.setCategory(category);
        product.setSupplier(supplier);

        return ProductResponse.from(productRepository.save(product));
    }

    @CacheEvict(value = HazelcastConfig.CACHE_PRODUCTS, key = "#id")
    @Transactional
    public void delete(UUID id) {
        Product product = productRepository.findActiveById(id)
            .orElseThrow(() -> new ProductNotFoundException(id));

        product.setActive(false);
        productRepository.save(product);
        log.info("Producto desactivado: {}", id);
    }

    @Cacheable(value = HazelcastConfig.CACHE_REPORTS, key = "'stock-stats'")
    @Transactional(readOnly = true)
    public StockStatsResponse getStockStats() {
        List<Product> allActive = productRepository.findAll(
            Specification.where(Specification.allOf())
        );

        int total = allActive.size();
        int belowMin = (int) allActive.stream()
            .filter(Product::isBelowMinimumStock)
            .count();

        List<Object[]> categoryStats = productRepository.findStockStatsByCategory();

        List<StockStatsResponse.CategoryStockStat> byCategory = categoryStats.stream()
            .map(row -> new StockStatsResponse.CategoryStockStat(
                (String) row[0],
                ((Number) row[1]).longValue(),
                ((Number) row[2]).longValue(),
                ((Number) row[3]).doubleValue()
            ))
            .toList();

        return new StockStatsResponse(total, total - belowMin, belowMin, byCategory);
    }

    public void checkAndSendStockAlerts() {
        List<Product> belowMin = productRepository.findProductsBelowMinimumStock();
        log.info("Revisando stock: {} productos por debajo del mínimo", belowMin.size());
        belowMin.forEach(this::sendStockAlert);
    }

    private void sendStockAlert(Product product) {
        stockAlertProducer.sendAlert(product);
        stockAlertCounter.increment();
        log.warn("Stock bajo - Producto: {} SKU: {} Stock: {}/{}",
            product.getName(), product.getSku(),
            product.getStockCurrent(), product.getStockMinimum()
        );
    }

    private Sort buildSort(String sortParam) {
        if (sortParam == null || sortParam.isBlank()) {
            return Sort.by(Sort.Direction.ASC, "name");
        }

        String[] parts = sortParam.split(",");
        String field = parts[0].trim();
        Sort.Direction direction = parts.length > 1 &&
            parts[1].trim().equalsIgnoreCase("desc")
            ? Sort.Direction.DESC
            : Sort.Direction.ASC;

        return Sort.by(direction, field);
    }

    private String getCurrentUser() {
        return SecurityContextHolder.getContext()
            .getAuthentication()
            .getName();
    }
}