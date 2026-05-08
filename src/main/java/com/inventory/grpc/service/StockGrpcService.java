package com.inventory.grpc.service;

import com.inventory.entity.Product;
import com.inventory.entity.StockMovement;
import com.inventory.enums.MovementType;
import com.inventory.grpc.generated.*;
import com.inventory.repository.ProductRepository;
import com.inventory.repository.StockMovementRepository;
import com.inventory.service.StockService;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.grpc.server.service.GrpcService;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class StockGrpcService extends StockServiceGrpc.StockServiceImplBase {

    private final StockService stockService;
    private final ProductRepository productRepository;
    private final StockMovementRepository stockMovementRepository;

    @Override
    public void getStockLevel(
            GetStockLevelRequest request,
            StreamObserver<StockLevelResponse> responseObserver) {
        try {
            UUID productId = UUID.fromString(request.getProductId());

            Product product = productRepository.findActiveById(productId)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Producto no encontrado: " + productId
                    ));

            StockLevelResponse response = StockLevelResponse.newBuilder()
                    .setProductId(product.getId().toString())
                    .setProductName(product.getName())
                    .setSku(product.getSku())
                    .setStockCurrent(product.getStockCurrent())
                    .setStockMinimum(product.getStockMinimum())
                    .setBelowMinimum(product.isBelowMinimumStock())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (IllegalArgumentException e) {
            responseObserver.onError(
                    Status.NOT_FOUND
                            .withDescription(e.getMessage())
                            .asRuntimeException()
            );
        } catch (Exception e) {
            log.error("Error en getStockLevel", e);
            responseObserver.onError(
                    Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException()
            );
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void listStockMovements(
            ListStockMovementsRequest request,
            StreamObserver<StockMovementListResponse> responseObserver) {
        try {
            int page = request.getPage();
            int size = request.getSize() == 0 ? 20 : request.getSize();
            PageRequest pageable = PageRequest.of(page, size);

            Page<StockMovement> movementsPage;

            if (!request.getProductId().isBlank()) {
                UUID productId = UUID.fromString(request.getProductId());
                movementsPage = stockMovementRepository
                        .findByProductId(productId, pageable);
            } else {
                // Sin filtro de producto — devuelve todos paginado
                movementsPage = stockMovementRepository.findAll(pageable);
            }

            List<StockMovementMessage> messages = movementsPage.getContent()
                    .stream()
                    .map(this::toProto)
                    .toList();

            StockMovementListResponse response = StockMovementListResponse.newBuilder()
                    .addAllMovements(messages)
                    .setTotalElements((int) movementsPage.getTotalElements())
                    .setTotalPages(movementsPage.getTotalPages())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("Error en listStockMovements", e);
            responseObserver.onError(
                    Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException()
            );
        }
    }

    @Override
    public void getStockStats(
            GetStockStatsRequest request,
            StreamObserver<StockStatsResponse> responseObserver) {
        try {
            // Movimientos de los últimos 30 días para gráficas
            LocalDateTime since = LocalDateTime.now().minusDays(30);
            List<Object[]> dailyStats = stockMovementRepository
                    .findDailyMovementStats(since);

            // Cuenta productos por estado de stock
            List<Product> allActive = productRepository
                    .findProductsBelowMinimumStock();

            // Construye los stats diarios
            List<DailyMovementStat> dailyMovementStats = dailyStats.stream()
                    .map(row -> DailyMovementStat.newBuilder()
                            .setDay(row[0].toString())
                            .setMovementType(row[1].toString())
                            .setTotalQuantity(((Number) row[2]).longValue())
                            .setTotalMovements(((Number) row[3]).longValue())
                            .build()
                    )
                    .toList();

            StockStatsResponse response = StockStatsResponse.newBuilder()
                    .setProductsBelowMinimum(allActive.size())
                    .addAllDailyMovements(dailyMovementStats)
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("Error en getStockStats", e);
            responseObserver.onError(
                    Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException()
            );
        }
    }

    @Override
    public void registerMovement(
            RegisterMovementRequest request,
            StreamObserver<RegisterMovementResponse> responseObserver) {
        try {
            UUID productId = UUID.fromString(request.getProductId());
            MovementType type = MovementType.valueOf(request.getMovementType());

            int stockAfter = stockService.registerMovement(
                    productId,
                    type,
                    request.getQuantity(),
                    request.getReferenceId().isBlank()
                            ? null : UUID.fromString(request.getReferenceId()),
                    request.getReferenceType(),
                    request.getNotes(),
                    request.getCreatedBy()
            );

            responseObserver.onNext(
                    RegisterMovementResponse.newBuilder()
                            .setSuccess(true)
                            .setStockAfter(stockAfter)
                            .setMessage("Movimiento registrado exitosamente")
                            .build()
            );
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("Error en registerMovement", e);
            responseObserver.onError(
                    Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException()
            );
        }
    }

    private StockMovementMessage toProto(StockMovement movement) {
        return StockMovementMessage.newBuilder()
                .setId(movement.getId().toString())
                .setProductId(movement.getProduct().getId().toString())
                .setProductName(movement.getProduct().getName())
                .setMovementType(movement.getMovementType().name())
                .setQuantity(movement.getQuantity())
                .setStockBefore(movement.getStockBefore())
                .setStockAfter(movement.getStockAfter())
                .setCreatedBy(movement.getCreatedBy())
                .setCreatedAt(movement.getCreatedAt().toString())
                .build();
    }
}