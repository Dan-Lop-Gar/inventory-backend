package com.inventory.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Public data of update product request")
public record UpdateProductRequest(
    @NotBlank(message = "Nombre es requerido")
    @Size(max = 200)
    @Schema(example = "glass")
    String name,

    @Schema(example = "description")
    String description,

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    @Digits(integer = 10, fraction = 2)
    @Schema(example = "384.1")
    BigDecimal price,

    @NotNull
    @Min(0)
    @Schema(example = "25")
    Integer stockMinimum,

    @NotNull
    @Schema(example = "334e05da-d1e2-476f-9fb1-90d555cdaf69")
    UUID categoryId,

    @NotNull
    @Schema(example = "334e05da-d1e2-476f-9fb1-90d555cdaf69")
    UUID supplierId,

    @NotNull
    @Schema(example = "false")
    Boolean active
) {}