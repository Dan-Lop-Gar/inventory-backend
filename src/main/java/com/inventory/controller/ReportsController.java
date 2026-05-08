package com.inventory.controller;

import com.inventory.dto.SalesReportResponse;
import com.inventory.dto.StockStatsResponse;
import com.inventory.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Tag(name = "Reports", description = "Reportes y estadísticas del sistema de inventario")
@SecurityRequirement(name = "bearerAuth")
public class ReportsController {

    private final ReportService reportService;

    @GetMapping("/stock-stats")
    @Operation(
        summary = "Estadísticas de stock por categoría",
        description = """
            Retorna estadísticas agregadas de stock agrupadas por categoría.
            Incluye totales de productos, stock acumulado y precio promedio.
            El resultado se cachea en Hazelcast por 2 minutos.
            Ideal para gráficas de dashboard.
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Estadísticas obtenidas exitosamente",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = StockStatsResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "403",
            description = "No tiene permisos. Requiere rol ADMIN, AUDITOR o WAREHOUSE",
            content = @Content(mediaType = "application/json")
        )
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'AUDITOR', 'WAREHOUSE')")
    public ResponseEntity<StockStatsResponse> getStockStats() {
        return ResponseEntity.ok(reportService.getStockStats());
    }

    @GetMapping("/sales")
    @Operation(
        summary = "Reporte de ventas por período",
        description = """
            Retorna un reporte de ventas entre dos fechas.
            Incluye totales, estadísticas diarias y productos más vendidos.
            
            **Parámetros:**
            - `dateFrom` — fecha inicial inclusive (formato ISO: `yyyy-MM-dd`)
            - `dateTo` — fecha final inclusive (formato ISO: `yyyy-MM-dd`)
            
            **Restricciones:**
            - `dateFrom` debe ser anterior o igual a `dateTo`
            - El período máximo recomendado es 90 días
            
            **Ejemplos:**
                GET /api/reports/sales?dateFrom=2024-01-01&dateTo=2024-01-31
                GET /api/reports/sales?dateFrom=2024-06-01&dateTo=2024-06-30
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Reporte generado exitosamente",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = SalesReportResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Fechas inválidas (formato incorrecto, dateFrom posterior a dateTo, o rango excedido)",
            content = @Content(mediaType = "application/json")
        ),
        @ApiResponse(
            responseCode = "403",
            description = "No tiene permisos. Requiere rol ADMIN o AUDITOR",
            content = @Content(mediaType = "application/json")
        )
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'AUDITOR')")
    public ResponseEntity<SalesReportResponse> getSalesReport(
            @Parameter(description = "Fecha inicial del período (ISO 8601: yyyy-MM-dd)", required = true, example = "2024-01-01")
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate dateFrom,

            @Parameter(description = "Fecha final del período (ISO 8601: yyyy-MM-dd)", required = true, example = "2024-01-31")
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate dateTo) {
        return ResponseEntity.ok(reportService.getSalesReport(dateFrom, dateTo));
    }

    @GetMapping("/stock-alerts")
    @Operation(
        summary = "Productos con stock bajo",
        description = """
            Retorna la lista de productos cuyo stock actual es menor o igual al stock mínimo.
            Incluye datos del producto, categoría, proveedor y niveles de stock.
            Útil para generar alertas de reabastecimiento.
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Lista de productos con stock bajo obtenida exitosamente",
            content = @Content(mediaType = "application/json")
        ),
        @ApiResponse(
            responseCode = "403",
            description = "No tiene permisos. Requiere rol ADMIN, AUDITOR o WAREHOUSE",
            content = @Content(mediaType = "application/json")
        )
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'AUDITOR', 'WAREHOUSE')")
    public ResponseEntity<?> getStockAlerts() {
        return ResponseEntity.ok(reportService.getStockAlerts());
    }
}