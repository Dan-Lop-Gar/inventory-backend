package com.inventory.service;

import com.inventory.entity.Product;
import com.inventory.entity.PurchaseOrder;
import com.inventory.entity.PurchaseOrderLine;
import com.inventory.entity.StockMovement;
import com.inventory.entity.Supplier;
import com.inventory.enums.MovementType;
import com.inventory.enums.OrderStatus;
import com.inventory.repository.OrderRepository;
import com.inventory.repository.ProductRepository;
import com.inventory.repository.StockMovementRepository;
import com.inventory.repository.SupplierRepository;
import com.inventory.specification.OrderSpecification;
import com.inventory.dto.CreateOrderRequest;
import com.inventory.dto.OrderFilterRequest;
import com.inventory.dto.OrderResponse;
import com.inventory.dto.PagedResponse;
import com.inventory.exception.InvalidOrderStateException;
import com.inventory.exception.OrderNotFoundException;
import com.inventory.exception.ProductNotFoundException;
import com.inventory.kafka.producer.OrderEventProducer;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final SupplierRepository supplierRepository;
    private final StockMovementRepository stockMovementRepository;
    private final OrderEventProducer orderEventProducer;
    private final Counter ordersProcessedCounter;
    private final Counter orderRetryCounter;

    public OrderService(
            OrderRepository orderRepository,
            ProductRepository productRepository,
            SupplierRepository supplierRepository,
            StockMovementRepository stockMovementRepository,
            OrderEventProducer orderEventProducer,
            MeterRegistry meterRegistry) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.supplierRepository = supplierRepository;
        this.stockMovementRepository = stockMovementRepository;
        this.orderEventProducer = orderEventProducer;
        this.ordersProcessedCounter = Counter.builder("inventory_orders_processed_total")
                .description("Total de órdenes procesadas")
                .register(meterRegistry);
        this.orderRetryCounter = Counter.builder("inventory_kafka_retry_total")
                .description("Total de reintentos de órdenes")
                .register(meterRegistry);
    }

    @Transactional(readOnly = true)
    public PagedResponse<OrderResponse> findAll(OrderFilterRequest filter) {
        Sort sort = Sort.by(
                filter.sort().endsWith("desc")
                        ? Sort.Direction.DESC : Sort.Direction.ASC,
                filter.sort().split(",")[0]
        );

        PageRequest pageable = PageRequest.of(filter.page(), filter.size(), sort);

        Specification<PurchaseOrder> spec = OrderSpecification.withFilters(
                filter.status(),
                filter.supplierId(),
                filter.createdBy(),
                filter.dateFrom(),
                filter.dateTo()
        );

        Page<OrderResponse> page = orderRepository.findAll(spec, pageable)
                .map(OrderResponse::from);

        return PagedResponse.from(page);
    }

    @Transactional(readOnly = true)
    public OrderResponse findById(UUID id) {
        return orderRepository.findByIdWithDetails(id)
                .map(OrderResponse::from)
                .orElseThrow(() -> new OrderNotFoundException(id));
    }

    @Transactional
    public OrderResponse create(CreateOrderRequest request) {
        String currentUser = getCurrentUser();
        log.info("Creando orden de compra para proveedor: {} por: {}",
                request.supplierId(), currentUser);

        Supplier supplier = supplierRepository.findById(request.supplierId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Proveedor no encontrado: " + request.supplierId()
                ));

        PurchaseOrder order = PurchaseOrder.builder()
                .orderNumber(generateOrderNumber())
                .supplier(supplier)
                .notes(request.notes())
                .createdBy(currentUser)
                .build();

        request.lines().forEach(lineRequest -> {
            Product product = productRepository.findById(lineRequest.productId())
                    .orElseThrow(() -> new ProductNotFoundException(lineRequest.productId()));

            PurchaseOrderLine line = PurchaseOrderLine.builder()
                    .product(product)
                    .quantity(lineRequest.quantity())
                    .unitPrice(lineRequest.unitPrice())
                    .build();

            order.addLine(line);
        });

        PurchaseOrder saved = orderRepository.save(order);
        log.info("Orden creada: {} - {}", saved.getId(), saved.getOrderNumber());

        // Publica evento en Kafka
        orderEventProducer.sendOrderEvent(saved, OrderStatus.PENDING);
        ordersProcessedCounter.increment();

        return OrderResponse.from(saved);
    }

    @Transactional
    public OrderResponse approve(UUID id) {
        String currentUser = getCurrentUser();
        PurchaseOrder order = getOrderOrThrow(id);

        if (!order.canBeApproved()) {
            throw new InvalidOrderStateException(
                    "Orden %s no puede ser aprobada en estado %s"
                            .formatted(order.getOrderNumber(), order.getStatus())
            );
        }

        order.setStatus(OrderStatus.APPROVED);
        order.setApprovedBy(currentUser);
        order.setApprovedAt(LocalDateTime.now());

        PurchaseOrder saved = orderRepository.save(order);
        orderEventProducer.sendOrderEvent(saved, OrderStatus.APPROVED);

        return OrderResponse.from(saved);
    }

    @Transactional
    public OrderResponse receive(UUID id) {
        PurchaseOrder order = getOrderOrThrow(id);

        if (!order.getStatus().canTransitionTo(OrderStatus.RECEIVED)) {
            throw new InvalidOrderStateException(
                    "Orden %s no puede ser recibida en estado %s"
                            .formatted(order.getOrderNumber(), order.getStatus())
            );
        }

        // Actualiza el stock de cada producto
        order.getLines().forEach(line -> {
            Product product = line.getProduct();
            int stockBefore = product.getStockCurrent();

            product.adjustStock(line.getQuantity());
            productRepository.save(product);

            // Registra el movimiento
            StockMovement movement = StockMovement.builder()
                    .product(product)
                    .movementType(MovementType.PURCHASE_IN)
                    .quantity(line.getQuantity())
                    .stockBefore(stockBefore)
                    .stockAfter(product.getStockCurrent())
                    .referenceId(order.getId())
                    .referenceType("PURCHASE_ORDER")
                    .createdBy(getCurrentUser())
                    .build();

            stockMovementRepository.save(movement);
        });

        order.setStatus(OrderStatus.RECEIVED);
        order.setReceivedAt(LocalDateTime.now());

        PurchaseOrder saved = orderRepository.save(order);
        orderEventProducer.sendOrderEvent(saved, OrderStatus.RECEIVED);

        return OrderResponse.from(saved);
    }

    @Transactional
    public OrderResponse cancel(UUID id) {
        PurchaseOrder order = getOrderOrThrow(id);

        if (!order.canBeCancelled()) {
            throw new InvalidOrderStateException(
                    "Orden %s no puede ser cancelada en estado %s"
                            .formatted(order.getOrderNumber(), order.getStatus())
            );
        }

        order.setStatus(OrderStatus.CANCELLED);
        PurchaseOrder saved = orderRepository.save(order);
        orderEventProducer.sendOrderEvent(saved, OrderStatus.CANCELLED);

        return OrderResponse.from(saved);
    }

    @Transactional
    public void markAsFailed(UUID id, String reason) {
        PurchaseOrder order = getOrderOrThrow(id);
        order.setStatus(OrderStatus.FAILED);
        order.setFailureReason(reason);
        order.setRetryCount(order.getRetryCount() + 1);
        orderRepository.save(order);
        orderRetryCounter.increment();
    }

    private PurchaseOrder getOrderOrThrow(UUID id) {
        return orderRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new OrderNotFoundException(id));
    }

    private String generateOrderNumber() {
        return "PO-%d-%s".formatted(
                System.currentTimeMillis(),
                UUID.randomUUID().toString().substring(0, 8).toUpperCase()
        );
    }

    private String getCurrentUser() {
        return SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();
    }
}