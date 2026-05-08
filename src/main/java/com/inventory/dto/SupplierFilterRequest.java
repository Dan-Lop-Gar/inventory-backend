package com.inventory.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@Schema(
    name = "SupplierFilterRequest",
    description = "Filtros opcionales para listar proveedores con paginación y ordenamiento"
)
public record SupplierFilterRequest(

    @Schema(
        description = "Filtra proveedores cuyo nombre contenga este texto (búsqueda parcial, case-insensitive)",
        example = "Acme",
        nullable = true
    )
    String name,

    @Schema(
        description = "Filtra proveedores por país exacto",
        example = "México",
        nullable = true
    )
    String country,

    @Schema(
        description = "Filtra por estado activo/inactivo. Por defecto: true",
        example = "true",
        defaultValue = "true",
        nullable = true
    )
    Boolean active,

    @Schema(
        description = "Número de página (base 0). Por defecto: 0",
        example = "0",
        defaultValue = "0",
        minimum = "0",
        nullable = true
    )
    @Min(0)
    Integer page,

    @Schema(
        description = "Cantidad de elementos por página. Máximo 100. Por defecto: 20",
        example = "20",
        defaultValue = "20",
        minimum = "1",
        maximum = "100",
        nullable = true
    )
    @Min(1)
    @Max(100)
    Integer size,

    @Schema(
        description = "Campo y dirección de ordenamiento separados por coma. Por defecto: name,asc",
        example = "name,asc",
        defaultValue = "name,asc",
        allowableValues = {
            "name,asc", "name,desc",
            "country,asc", "country,desc",
            "createdAt,asc", "createdAt,desc"
        },
        nullable = true
    )
    String sort
) {
    public SupplierFilterRequest {
        if (page == null)   page = 0;
        if (size == null)   size = 20;
        if (sort == null)   sort = "name,asc";
        if (active == null) active = true;
    }
}
