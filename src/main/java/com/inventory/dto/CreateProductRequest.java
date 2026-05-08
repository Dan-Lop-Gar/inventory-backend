package com.inventory.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Public data of create product request")
public record CreateProductRequest(
    @NotBlank(message = "SKU es requerido")
    @Size(max = 50, message = "SKU máximo 50 caracteres")
    @Schema(example = "sku-123")
    String sku,

    @NotBlank(message = "Nombre es requerido")
    @Size(max = 200, message = "Nombre máximo 200 caracteres")
    @Schema(example = "glass")
    String name,

    @Schema(example = "description")
    String description,

    @NotNull(message = "Precio es requerido")
    @DecimalMin(value = "0.0", inclusive = false, message = "Precio debe ser mayor a 0")
    @Digits(integer = 10, fraction = 2, message = "Precio inválido")
    @Schema(example = "384.1")
    BigDecimal price,

    @NotNull(message = "Stock inicial es requerido")
    @Min(value = 0, message = "Stock inicial mínimo 0")
    @Schema(example = "321")
    Integer stockCurrent,

    @NotNull(message = "Stock mínimo es requerido")
    @Min(value = 0, message = "Stock mínimo debe ser 0 o mayor")
    @Schema(example = "25")
    Integer stockMinimum,

    @NotNull(message = "Categoría es requerida")
    @Schema(example = "334e05da-d1e2-476f-9fb1-90d555cdaf69")
    UUID categoryId,

    @NotNull(message = "Proveedor es requerido")
    @Schema(example = "334e05da-d1e2-476f-9fb1-90d555cdaf69")
    UUID supplierId
) {}