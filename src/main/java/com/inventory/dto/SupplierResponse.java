package com.inventory.dto;

import com.inventory.entity.Supplier;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(
    name = "SupplierResponse",
    description = "Respuesta con el detalle completo de un proveedor del sistema"
)
public record SupplierResponse(

    @Schema(
        description = "Identificador único del proveedor",
        example = "550e8400-e29b-41d4-a716-446655440000",
        accessMode = Schema.AccessMode.READ_ONLY
    )
    UUID id,

    @Schema(
        description = "Nombre comercial del proveedor",
        example = "Acme Corporation S.A. de C.V."
    )
    String name,

    @Schema(
        description = "Correo electrónico de contacto del proveedor",
        example = "contacto@acmecorp.com"
    )
    String email,

    @Schema(
        description = "Teléfono de contacto del proveedor",
        example = "+52 55 1234 5678",
        nullable = true
    )
    String phone,

    @Schema(
        description = "País donde opera el proveedor",
        example = "México",
        nullable = true
    )
    String country,

    @Schema(
        description = "Dirección física o fiscal del proveedor",
        example = "Av. Insurgentes Sur 1605, Ciudad de México, CDMX",
        nullable = true
    )
    String address,

    @Schema(
        description = "Indica si el proveedor está activo en el sistema. Los proveedores inactivos no pueden ser usados en nuevas órdenes",
        example = "true"
    )
    Boolean active,

    @Schema(
        description = "Fecha y hora de creación del registro",
        example = "2024-01-15T10:30:00",
        type = "string",
        format = "date-time",
        accessMode = Schema.AccessMode.READ_ONLY
    )
    LocalDateTime createdAt,

    @Schema(
        description = "Fecha y hora de última actualización del registro",
        example = "2024-03-20T14:45:00",
        type = "string",
        format = "date-time",
        accessMode = Schema.AccessMode.READ_ONLY
    )
    LocalDateTime updatedAt
) implements Serializable {
    private static final long serialVersionUID = 1L;

    public static SupplierResponse from(Supplier supplier) {
        return new SupplierResponse(
                supplier.getId(),
                supplier.getName(),
                supplier.getEmail(),
                supplier.getPhone(),
                supplier.getCountry(),
                supplier.getAddress(),
                supplier.getActive(),
                supplier.getCreatedAt(),
                supplier.getUpdatedAt()
        );
    }
}
