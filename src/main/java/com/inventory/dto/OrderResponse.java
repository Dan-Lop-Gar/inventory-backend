package com.inventory.dto;

import com.inventory.entity.PurchaseOrder;
import com.inventory.entity.PurchaseOrderLine;
import com.inventory.enums.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Schema(
    name = "OrderResponse",
    description = "Respuesta con el detalle completo de una orden de compra, incluyendo líneas, proveedor y totales"
)
public record OrderResponse(

    @Schema(
        description = "Identificador único de la orden",
        example = "550e8400-e29b-41d4-a716-446655440000",
        accessMode = Schema.AccessMode.READ_ONLY
    )
    UUID id,

    @Schema(
        description = "Número legible de la orden (ej: ORD-2024-0001)",
        example = "ORD-2024-0156"
    )
    String orderNumber,

    @Schema(
        description = "Estado actual de la orden",
        example = "PENDING",
        allowableValues = {"PENDING", "APPROVED", "RECEIVED", "CANCELLED"}
    )
    OrderStatus status,

    @Schema(
        description = "Nombre del proveedor asociado",
        example = "Acme Corporation S.A. de C.V."
    )
    String supplierName,

    @Schema(
        description = "Identificador único del proveedor",
        example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
    )
    UUID supplierId,

    @Schema(
        description = "Monto total de la orden (suma de todas las líneas)",
        example = "4999.95"
    )
    BigDecimal totalAmount,

    @Schema(
        description = "Notas o comentarios adicionales de la orden",
        example = "Entregar en almacén principal, horario 9:00-14:00"
    )
    String notes,

    @Schema(
        description = "Usuario que creó la orden",
        example = "juan.perez@inventory.com"
    )
    String createdBy,

    @Schema(
        description = "Usuario que aprobó la orden (null si aún no está aprobada)",
        example = "maria.garcia@inventory.com",
        nullable = true
    )
    String approvedBy,

    @Schema(
        description = "Contador de reintentos de procesamiento (0 para órdenes nuevas)",
        example = "0",
        minimum = "0"
    )
    Integer retryCount,

    @Schema(
        description = "Líneas de la orden (productos, cantidades y precios)",
        minLength = 1
    )
    List<OrderLineResponse> lines,

    @Schema(
        description = "Fecha y hora de creación de la orden",
        example = "2024-01-15T10:30:00",
        type = "string",
        format = "date-time"
    )
    LocalDateTime createdAt,

    @Schema(
        description = "Fecha y hora de aprobación (null si no está aprobada)",
        example = "2024-01-16T14:20:00",
        type = "string",
        format = "date-time",
        nullable = true
    )
    LocalDateTime approvedAt,

    @Schema(
        description = "Fecha y hora de recepción de mercancía (null si no fue recibida)",
        example = "2024-01-20T09:15:00",
        type = "string",
        format = "date-time",
        nullable = true
    )
    LocalDateTime receivedAt
) {
    public static OrderResponse from(PurchaseOrder order) {
        return new OrderResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getStatus(),
                order.getSupplier().getName(),
                order.getSupplier().getId(),
                order.getTotalAmount(),
                order.getNotes(),
                order.getCreatedBy(),
                order.getApprovedBy(),
                order.getRetryCount(),
                order.getLines().stream()
                        .map(OrderLineResponse::from)
                        .toList(),
                order.getCreatedAt(),
                order.getApprovedAt(),
                order.getReceivedAt()
        );
    }

    @Schema(
        name = "OrderLineResponse",
        description = "Línea individual de una orden de compra con detalle del producto"
    )
    public record OrderLineResponse(

        @Schema(
            description = "Identificador único de la línea",
            example = "660e8400-e29b-41d4-a716-446655440001"
        )
        UUID id,

        @Schema(
            description = "Identificador del producto",
            example = "770e8400-e29b-41d4-a716-446655440002"
        )
        UUID productId,

        @Schema(
            description = "Nombre del producto",
            example = "Laptop Dell Latitude 5430"
        )
        String productName,

        @Schema(
            description = "SKU del producto",
            example = "LAP-DEL-5430"
        )
        String productSku,

        @Schema(
            description = "Cantidad ordenada",
            example = "5",
            minimum = "1"
        )
        Integer quantity,

        @Schema(
            description = "Precio unitario al momento de la orden",
            example = "999.99"
        )
        BigDecimal unitPrice,

        @Schema(
            description = "Precio total de la línea (quantity × unitPrice)",
            example = "4999.95"
        )
        BigDecimal totalPrice
    ) {
        public static OrderLineResponse from(PurchaseOrderLine line) {
            return new OrderLineResponse(
                    line.getId(),
                    line.getProduct().getId(),
                    line.getProduct().getName(),
                    line.getProduct().getSku(),
                    line.getQuantity(),
                    line.getUnitPrice(),
                    line.getTotalPrice()
            );
        }
    }
}
