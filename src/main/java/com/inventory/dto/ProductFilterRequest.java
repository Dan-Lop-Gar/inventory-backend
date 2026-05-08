package com.inventory.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Public data of product filter request")
public record ProductFilterRequest(
    @Schema(example = "glass")
    String name,

    @Schema(example = "sku-123")
    String sku,

    @Schema(example = "334e05da-d1e2-476f-9fb1-90d555cdaf69")
    UUID categoryId,

    @Schema(example = "334e05da-d1e2-476f-9fb1-90d555cdaf69")
    UUID supplierId,

    @DecimalMin(value = "0.0", message = "Precio mínimo debe ser 0 o mayor")
    @Schema(example = "24.56")
    BigDecimal minPrice,

    @DecimalMin(value = "0.0", message = "Precio máximo debe ser 0 o mayor")
    @Schema(example = "324.56")
    BigDecimal maxPrice,

    @Schema(example = "false")
    Boolean active,

    @Schema(example = "20")
    Boolean belowMinimumStock,

    @Min(value = 0, message = "Página mínima es 0")
    @Schema(example = "1")
    Integer page,

    @Min(value = 1, message = "Tamaño mínimo es 1")
    @Max(value = 100, message = "Tamaño máximo es 100")
    @Schema(example = "10")
    Integer size,

    @Schema(example = "name,asc")
    String sort
) {
    // Valores por defecto con compact constructor
    public ProductFilterRequest {
        if (page == null) page = 0;
        if (size == null) size = 20;
        if (sort == null) sort = "name,asc";
        if (active == null) active = true;
    }
}