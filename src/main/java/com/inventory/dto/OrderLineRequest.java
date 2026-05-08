package com.inventory.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Public data of  order line request")
public record OrderLineRequest(
    @NotNull(message = "Producto es requerido")
    @Schema(example = "334e05da-d1e2-476f-9fb1-90d555cdaf69")
    UUID productId,

    @NotNull(message = "Cantidad es requerida")
    @Min(value = 1, message = "Cantidad mínima es 1")
    @Schema(example = "56")
    Integer quantity,

    @NotNull(message = "Precio unitario es requerido")
    @DecimalMin(value = "0.0", inclusive = false)
    @Schema(example = "22.35")
    BigDecimal unitPrice
) {}