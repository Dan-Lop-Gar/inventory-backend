package com.inventory.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;

import java.io.Serializable;
import java.util.List;

@Schema(
    name = "PagedResponse",
    description = "Respuesta paginada genérica que envuelve una lista de elementos con metadatos de paginación"
)
public record PagedResponse<T>(

    @Schema(
        description = "Lista de elementos de la página actual",
        minLength = 0,
        maxLength = 100
    )
    List<T> content,

    @Schema(
        description = "Número de página actual (base 0)",
        example = "0",
        minimum = "0"
    )
    int page,

    @Schema(
        description = "Cantidad de elementos por página",
        example = "20",
        minimum = "1",
        maximum = "100"
    )
    int size,

    @Schema(
        description = "Total de elementos en todas las páginas",
        example = "156",
        minimum = "0"
    )
    long totalElements,

    @Schema(
        description = "Total de páginas disponibles",
        example = "8",
        minimum = "0"
    )
    int totalPages,

    @Schema(
        description = "Indica si es la primera página",
        example = "true"
    )
    boolean first,

    @Schema(
        description = "Indica si es la última página",
        example = "false"
    )
    boolean last
) implements Serializable {
    private static final long serialVersionUID = 1L;
    public static <T> PagedResponse<T> from(Page<T> page) {
        return new PagedResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
    }
}
