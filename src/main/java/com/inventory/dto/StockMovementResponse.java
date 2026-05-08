package com.inventory.dto;

import com.inventory.entity.StockMovement;
import com.inventory.enums.MovementType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(
    name = "StockMovementResponse",
    description = "Registro histórico de un movimiento de stock (entrada, salida o ajuste) con detalle del producto y estado antes/después"
)
public record StockMovementResponse(

    @Schema(
        description = "Identificador único del movimiento",
        example = "550e8400-e29b-41d4-a716-446655440000",
        accessMode = Schema.AccessMode.READ_ONLY
    )
    UUID id,

    @Schema(
        description = "Identificador del producto afectado",
        example = "660e8400-e29b-41d4-a716-446655440001"
    )
    UUID productId,

    @Schema(
        description = "Nombre del producto afectado",
        example = "Laptop Dell Latitude 5430"
    )
    String productName,

    @Schema(
        description = "Código SKU del producto afectado",
        example = "LAP-DEL-5430"
    )
    String productSku,

    @Schema(
        description = "Tipo de movimiento de stock",
        example = "SALE_OUT",
        allowableValues = {"PURCHASE_IN", "SALE_OUT", "ADJUSTMENT_IN", "ADJUSTMENT_OUT", "RETURN_IN"}
    )
    MovementType movementType,

    @Schema(
        description = "Indica si el movimiento aumenta el stock (entrada) o lo disminuye (salida)",
        example = "false",
        accessMode = Schema.AccessMode.READ_ONLY
    )
    boolean inbound,

    @Schema(
        description = "Cantidad de unidades movidas (siempre positiva)",
        example = "5",
        minimum = "1"
    )
    int quantity,

    @Schema(
        description = "Stock disponible antes de aplicar el movimiento",
        example = "25",
        minimum = "0"
    )
    int stockBefore,

    @Schema(
        description = "Stock disponible después de aplicar el movimiento",
        example = "20",
        minimum = "0"
    )
    int stockAfter,

    @Schema(
        description = "Identificador de la entidad que originó el movimiento (ej: ID de orden, ID de ajuste)",
        example = "770e8400-e29b-41d4-a716-446655440002",
        nullable = true
    )
    UUID referenceId,

    @Schema(
        description = "Tipo de entidad que originó el movimiento",
        example = "PURCHASE_ORDER",
        allowableValues = {"PURCHASE_ORDER", "SALE_ORDER", "ADJUSTMENT", "RETURN"},
        nullable = true
    )
    String referenceType,

    @Schema(
        description = "Notas o comentarios sobre el movimiento",
        example = "Ajuste por inventario físico: faltante detectado",
        nullable = true
    )
    String notes,

    @Schema(
        description = "Usuario que registró el movimiento",
        example = "juan.perez@inventory.com"
    )
    String createdBy,

    @Schema(
        description = "Fecha y hora en que se registró el movimiento",
        example = "2024-01-15T14:30:00",
        type = "string",
        format = "date-time",
        accessMode = Schema.AccessMode.READ_ONLY
    )
    LocalDateTime createdAt
) {
    public static StockMovementResponse from(StockMovement movement) {
        return new StockMovementResponse(
                movement.getId(),
                movement.getProduct().getId(),
                movement.getProduct().getName(),
                movement.getProduct().getSku(),
                movement.getMovementType(),
                movement.getMovementType().isInbound(),
                movement.getQuantity(),
                movement.getStockBefore(),
                movement.getStockAfter(),
                movement.getReferenceId(),
                movement.getReferenceType(),
                movement.getNotes(),
                movement.getCreatedBy(),
                movement.getCreatedAt()
        );
    }
}
