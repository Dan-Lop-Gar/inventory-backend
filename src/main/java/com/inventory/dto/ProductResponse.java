package com.inventory.dto;

import com.inventory.entity.Product;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(
    name = "ProductResponse",
    description = "Respuesta con el detalle completo de un producto del inventario, incluyendo stock, categoría y proveedor"
)
public record ProductResponse(

    @Schema(
        description = "Identificador único del producto",
        example = "550e8400-e29b-41d4-a716-446655440000",
        accessMode = Schema.AccessMode.READ_ONLY
    )
    UUID id,

    @Schema(
        description = "Código SKU único del producto (Stock Keeping Unit)",
        example = "LAP-DEL-5430"
    )
    String sku,

    @Schema(
        description = "Nombre del producto",
        example = "Laptop Dell Latitude 5430"
    )
    String name,

    @Schema(
        description = "Descripción detallada del producto",
        example = "Laptop empresarial 14 pulgadas, Intel Core i7, 16GB RAM, 512GB SSD",
        nullable = true
    )
    String description,

    @Schema(
        description = "Precio de venta del producto",
        example = "999.99",
        minimum = "0"
    )
    BigDecimal price,

    @Schema(
        description = "Stock actual disponible en inventario",
        example = "25",
        minimum = "0"
    )
    Integer stockCurrent,

    @Schema(
        description = "Stock mínimo permitido antes de generar alerta",
        example = "10",
        minimum = "0"
    )
    Integer stockMinimum,

    @Schema(
        description = "Indica si el producto está activo en el catálogo",
        example = "true"
    )
    Boolean active,

    @Schema(
        description = "Indica si el stock actual está en nivel crítico (stockCurrent <= stockMinimum)",
        example = "false",
        accessMode = Schema.AccessMode.READ_ONLY
    )
    Boolean belowMinimumStock,

    @Schema(
        description = "Nombre de la categoría a la que pertenece el producto",
        example = "Electrónica"
    )
    String categoryName,

    @Schema(
        description = "Nombre del proveedor que suministra el producto",
        example = "Acme Corporation S.A. de C.V."
    )
    String supplierName,

    @Schema(
        description = "Fecha y hora de creación del producto",
        example = "2024-01-15T10:30:00",
        type = "string",
        format = "date-time",
        accessMode = Schema.AccessMode.READ_ONLY
    )
    LocalDateTime createdAt,

    @Schema(
        description = "Fecha y hora de última actualización del producto",
        example = "2024-03-20T14:45:00",
        type = "string",
        format = "date-time",
        accessMode = Schema.AccessMode.READ_ONLY
    )
    LocalDateTime updatedAt
) implements Serializable {
    private static final long serialVersionUID = 1L;

    public static ProductResponse from(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getSku(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getStockCurrent(),
                product.getStockMinimum(),
                product.getActive(),
                product.isBelowMinimumStock(),
                product.getCategory().getName(),
                product.getSupplier().getName(),
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }
}
