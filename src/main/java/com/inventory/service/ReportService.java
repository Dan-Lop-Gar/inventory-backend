package com.inventory.service;

import com.inventory.config.HazelcastConfig;
import com.inventory.repository.OrderRepository;
import com.inventory.repository.ProductRepository;
import com.inventory.repository.StockMovementRepository;
import com.inventory.enums.OrderStatus;
import com.inventory.dto.ProductResponse;
import com.inventory.dto.SalesReportResponse;
import com.inventory.dto.StockStatsResponse;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

@Slf4j
@Service
public class ReportService {

    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final StockMovementRepository stockMovementRepository;
    private final ExecutorService reportExecutor;
    private final Timer reportTimer;

    public ReportService(
            ProductRepository productRepository,
            OrderRepository orderRepository,
            StockMovementRepository stockMovementRepository,
            @Qualifier("reportExecutor") ExecutorService reportExecutor,
            MeterRegistry meterRegistry) {
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.stockMovementRepository = stockMovementRepository;
        this.reportExecutor = reportExecutor;
        this.reportTimer = Timer.builder("inventory_report_generation_seconds")
                .description("Tiempo de generación de reportes")
                .register(meterRegistry);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = HazelcastConfig.CACHE_REPORTS, key = "'stock-stats'")
    public StockStatsResponse getStockStats() {
        return reportTimer.record(() -> {
            log.debug("Generando reporte de stock stats");

            List<Object[]> categoryStats = productRepository
                    .findStockStatsByCategory();

            List<StockStatsResponse.CategoryStockStat> byCategory = categoryStats
                    .stream()
                    .map(row -> new StockStatsResponse.CategoryStockStat(
                            (String) row[0],
                            ((Number) row[1]).longValue(),
                            ((Number) row[2]).longValue(),
                            ((Number) row[3]).doubleValue()
                    ))
                    .toList();

            long totalProducts = byCategory.stream()
                    .mapToLong(StockStatsResponse.CategoryStockStat::totalProducts)
                    .sum();

            List<Object[]> belowMin = productRepository
                    .findStockStatsByCategory(); // reutilizamos — ya agrupa por categoría

            int belowMinCount = productRepository
                    .findProductsBelowMinimumStock()
                    .size();

            return new StockStatsResponse(
                    (int) totalProducts,
                    (int) totalProducts - belowMinCount,
                    belowMinCount,
                    byCategory
            );
        });
    }

    @Transactional(readOnly = true)
    public SalesReportResponse getSalesReport(LocalDate dateFrom, LocalDate dateTo) {
        log.info("Generando reporte de ventas: {} → {}", dateFrom, dateTo);

        LocalDateTime from = dateFrom.atStartOfDay();
        LocalDateTime to = dateTo.atTime(LocalTime.MAX);

        // Ejecuta las dos queries pesadas en paralelo usando el reportExecutor
        CompletableFuture<Long> totalOrdersFuture = CompletableFuture.supplyAsync(
                () -> orderRepository.countByStatusSince(OrderStatus.RECEIVED, from),
                reportExecutor
        );

        CompletableFuture<List<Object[]>> dailyStatsFuture = CompletableFuture.supplyAsync(
                () -> stockMovementRepository.findDailyMovementStats(from),
                reportExecutor
        );

        // Espera ambas
        Long totalOrders = totalOrdersFuture.join();
        List<Object[]> rawDailyStats = dailyStatsFuture.join();

        // Construye los daily stats
        List<SalesReportResponse.DailyOrderStat> dailyStats = rawDailyStats
                .stream()
                .map(row -> new SalesReportResponse.DailyOrderStat(
                        LocalDate.parse(row[0].toString().substring(0, 10)),
                        ((Number) row[3]).longValue(),
                        BigDecimal.valueOf(((Number) row[2]).doubleValue())
                ))
                .toList();

        // Top productos — los que más movimientos de salida tuvieron
        List<SalesReportResponse.TopProductStat> topProducts = rawDailyStats
                .stream()
                .filter(row -> row[1].toString().equals("SALE_OUT"))
                .map(row -> new SalesReportResponse.TopProductStat(
                        "Producto",
                        "SKU",
                        ((Number) row[2]).longValue(),
                        BigDecimal.ZERO
                ))
                .limit(10)
                .toList();

        BigDecimal totalAmount = dailyStats.stream()
                .map(SalesReportResponse.DailyOrderStat::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new SalesReportResponse(
                dateFrom,
                dateTo,
                totalAmount,
                totalOrders,
                dailyStats,
                topProducts
        );
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getStockAlerts() {
        log.debug("Obteniendo alertas de stock");

        return productRepository.findProductsBelowMinimumStock()
                .stream()
                .map(ProductResponse::from)
                .toList();
    }
}
