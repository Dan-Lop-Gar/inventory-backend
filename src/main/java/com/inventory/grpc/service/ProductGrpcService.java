package com.inventory.grpc.service;

import com.inventory.dto.ProductFilterRequest;
import com.inventory.grpc.generated.*;
import com.inventory.service.ProductService;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.grpc.server.service.GrpcService;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class ProductGrpcService extends ProductServiceGrpc.ProductServiceImplBase {

    private final ProductService productService;

    @Override
    public void getProduct(
            GetProductRequest request,
            StreamObserver<ProductResponse> responseObserver) {
        try {
            UUID productId = UUID.fromString(request.getProductId());
            var response = productService.findById(productId);

            responseObserver.onNext(toProto(response));
            responseObserver.onCompleted();

        } catch (IllegalArgumentException e) {
            responseObserver.onError(
                Status.NOT_FOUND
                    .withDescription("Producto no encontrado: " + request.getProductId())
                    .asRuntimeException()
            );
        } catch (Exception e) {
            log.error("Error en getProduct", e);
            responseObserver.onError(
                Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException()
            );
        }
    }

    @Override
    public void listProducts(
            ListProductsRequest request,
            StreamObserver<ProductListResponse> responseObserver) {
        try {
            ProductFilterRequest filter = new ProductFilterRequest(
                request.getName().isBlank() ? null : request.getName(),
                request.getSku().isBlank() ? null : request.getSku(),
                request.getCategoryId().isBlank() ? null : UUID.fromString(request.getCategoryId()),
                null,
                null,
                null,
                true,
                request.getBelowMinimumStock(),
                request.getPage(),
                request.getSize() == 0 ? 20 : request.getSize(),
                "name,asc"
            );

            var pagedResponse = productService.findAll(filter);

            ProductListResponse response = ProductListResponse.newBuilder()
                .addAllProducts(
                    pagedResponse.content().stream()
                        .map(this::toProto)
                        .toList()
                )
                .setTotalElements((int) pagedResponse.totalElements())
                .setTotalPages(pagedResponse.totalPages())
                .setCurrentPage(pagedResponse.page())
                .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("Error en listProducts", e);
            responseObserver.onError(
                Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException()
            );
        }
    }

    @Override
    public void createProduct(
            CreateProductRequest request,
            StreamObserver<ProductResponse> responseObserver) {
        try {
            var dto = new com.inventory.dto.CreateProductRequest(
                request.getSku(),
                request.getName(),
                request.getDescription(),
                BigDecimal.valueOf(request.getPrice()),
                request.getStockCurrent(),
                request.getStockMinimum(),
                UUID.fromString(request.getCategoryId()),
                UUID.fromString(request.getSupplierId())
            );

            var created = productService.create(dto);

            responseObserver.onNext(toProto(created));
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("Error en createProduct", e);
            responseObserver.onError(
                Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException()
            );
        }
    }

    @Override
    public void deleteProduct(
            DeleteProductRequest request,
            StreamObserver<DeleteProductResponse> responseObserver) {
        try {
            UUID id = UUID.fromString(request.getProductId());
            productService.delete(id);

            responseObserver.onNext(
                DeleteProductResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage("Producto eliminado: " + request.getProductId())
                    .build()
            );
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("Error en deleteProduct", e);
            responseObserver.onError(
                Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException()
            );
        }
    }

    @Override
    public void getProductStats(
            GetProductStatsRequest request,
            StreamObserver<ProductStatsResponse> responseObserver) {
        try {
            var stats = productService.getStockStats();

            ProductStatsResponse response = ProductStatsResponse.newBuilder()
                .setTotalProducts(stats.totalProducts())
                .setProductsAboveMinimum(stats.productsAboveMinimum())
                .setProductsBelowMinimum(stats.productsBelowMinimum())
                .addAllByCategory(
                    stats.byCategory().stream()
                        .map(cat -> CategoryStat.newBuilder()
                            .setCategory(cat.category())
                            .setTotalProducts(cat.totalProducts())
                            .setTotalStock(cat.totalStock())
                            .setAvgPrice(cat.avgPrice())
                            .build()
                        )
                        .toList()
                )
                .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("Error en getProductStats", e);
            responseObserver.onError(
                Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException()
            );
        }
    }

    private ProductResponse toProto(
            com.inventory.dto.ProductResponse dto) {
        return ProductResponse.newBuilder()
            .setId(dto.id().toString())
            .setSku(dto.sku())
            .setName(dto.name())
            .setDescription(dto.description() != null ? dto.description() : "")
            .setPrice(dto.price().doubleValue())
            .setStockCurrent(dto.stockCurrent())
            .setStockMinimum(dto.stockMinimum())
            .setActive(dto.active())
            .setBelowMinimumStock(dto.belowMinimumStock())
            .setCategoryName(dto.categoryName())
            .setSupplierName(dto.supplierName())
            .build();
    }
}