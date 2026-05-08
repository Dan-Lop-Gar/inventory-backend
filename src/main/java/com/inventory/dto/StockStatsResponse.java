package com.inventory.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(
    name = "StockStatsResponse",
    description = "Estadísticas consolidadas de stock del inventario, incluyendo totales generales y desglose por categoría"
)
public record StockStatsResponse(

    @Schema(
        description = "Cantidad total de productos activos en el inventario",
        example = "156",
        minimum = "0"
    )
    int totalProducts,

    @Schema(
        description = "Cantidad de productos con stock por encima del mínimo establecido",
        example = "142",
        minimum = "0"
    )
    int productsAboveMinimum,

    @Schema(
        description = "Cantidad de productos con stock en nivel crítico (stock actual <= stock mínimo)",
        example = "14",
        minimum = "0"
    )
    int productsBelowMinimum,

    @Schema(
        description = "Desglose de estadísticas por categoría de producto",
        minLength = 0
    )
    List<CategoryStockStat> byCategory
) {

    @Schema(
        name = "CategoryStockStat",
        description = "Estadísticas de stock para una categoría específica"
    )
    public record CategoryStockStat(

        @Schema(
            description = "Nombre de la categoría",
            example = "Electrónica"
        )
        String category,

        @Schema(
            description = "Cantidad de productos en esta categoría",
            example = "25",
            minimum = "0"
        )
        long totalProducts,

        @Schema(
            description = "Suma total de unidades en stock para todos los productos de la categoría",
            example = "450",
            minimum = "0"
        )
        long totalStock,

        @Schema(
            description = "Precio promedio de los productos en la categoría",
            example = "899.99"
        )
        double avgPrice
    ) {}
}
