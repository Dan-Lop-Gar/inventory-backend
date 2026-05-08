package com.inventory.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Schema(
    name = "SalesReportResponse",
    description = "Reporte de ventas consolidado por período, incluyendo estadísticas diarias y productos más vendidos"
)
public record SalesReportResponse(

    @Schema(
        description = "Fecha inicial del período del reporte",
        example = "2024-01-01",
        type = "string",
        format = "date"
    )
    LocalDate dateFrom,

    @Schema(
        description = "Fecha final del período del reporte",
        example = "2024-01-31",
        type = "string",
        format = "date"
    )
    LocalDate dateTo,

    @Schema(
        description = "Monto total de ventas en el período",
        example = "125750.50"
    )
    BigDecimal totalAmount,

    @Schema(
        description = "Cantidad total de órdenes en el período",
        example = "156",
        minimum = "0"
    )
    long totalOrders,

    @Schema(
        description = "Estadísticas de ventas agrupadas por día",
        minLength = 0
    )
    List<DailyOrderStat> dailyStats,

    @Schema(
        description = "Top productos más vendidos en el período",
        minLength = 0,
        maxLength = 10
    )
    List<TopProductStat> topProducts
) {

    @Schema(
        name = "DailyOrderStat",
        description = "Estadística de ventas para un día específico"
    )
    public record DailyOrderStat(

        @Schema(
            description = "Fecha del día",
            example = "2024-01-15",
            type = "string",
            format = "date"
        )
        LocalDate date,

        @Schema(
            description = "Cantidad de órdenes realizadas ese día",
            example = "12",
            minimum = "0"
        )
        long ordersCount,

        @Schema(
            description = "Monto total de ventas del día",
            example = "8540.00"
        )
        BigDecimal amount
    ) {}

    @Schema(
        name = "TopProductStat",
        description = "Estadística de un producto dentro del top de más vendidos"
    )
    public record TopProductStat(

        @Schema(
            description = "Nombre del producto",
            example = "Laptop Dell Latitude 5430"
        )
        String productName,

        @Schema(
            description = "Código SKU del producto",
            example = "LAP-DEL-5430"
        )
        String sku,

        @Schema(
            description = "Cantidad total vendida en el período",
            example = "45",
            minimum = "0"
        )
        long totalQuantity,

        @Schema(
            description = "Monto total generado por las ventas del producto",
            example = "44995.55"
        )
        BigDecimal totalAmount
    ) {}
}
