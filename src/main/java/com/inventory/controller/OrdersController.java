package com.inventory.controller;

import com.inventory.dto.CreateOrderRequest;
import com.inventory.dto.OrderFilterRequest;
import com.inventory.dto.OrderResponse;
import com.inventory.dto.PagedResponse;
import com.inventory.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Tag(name = "Orders", description = "Gestión de órdenes de compra a proveedores")
@SecurityRequirement(name = "bearerAuth")
public class OrdersController {

    private final OrderService orderService;

    @GetMapping
    @Operation(
            summary = "Listar órdenes de compra",
            description = """
        Retorna una página paginada de órdenes de compra. Todos los filtros son opcionales y se combinan con AND.
        
        **Filtros disponibles:**
        - `status` — estado de la orden: `PENDING | APPROVED | RECEIVED | CANCELLED`
        - `supplierId` — UUID del proveedor (ej: `550e8400-e29b-41d4-a716-446655440000`)
        - `createdBy` — usuario que creó la orden (ej: `juan.perez`)
        - `dateFrom` / `dateTo` — rango de fechas ISO 8601 (ej: `2024-01-15T10:30:00`)
        - `minPrice` / `maxPrice` — rango de monto total de la orden
        - `active` — filtra por estado activo/inactivo (default: `true`)
        - `belowMinimumStock` — filtra órdenes con productos bajo stock mínimo
        
        **Paginación:**
        - `page` — número de página, base 0 (default: `0`)
        - `size` — tamaño de página, máximo 100 (default: `20`)
        
        **Ordenamiento:**
        - `sort` — campo y dirección separados por coma (default: `createdAt,desc`)
        - Campos disponibles: `id | orderNumber | status | totalAmount | createdAt | approvedAt | receivedAt`
        - Direcciones: `asc | desc`
        
        **Ejemplos:**
            GET /api/orders?status=PENDING&page=0&size=10
            GET /api/orders?supplierId=550e8400-e29b-41d4-a716-446655440000&dateFrom=2024-01-01T00:00:00
            GET /api/orders?minPrice=1000.00&maxPrice=5000.00&sort=totalAmount,desc
            GET /api/orders?createdBy=juan.perez&status=APPROVED&page=0&size=50
        """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Lista de órdenes obtenida exitosamente",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = PagedResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Parámetros de filtro inválidos (fecha mal formada, UUID inválido, página negativa, etc.)",
            content = @Content(mediaType = "application/json")
        ),
        @ApiResponse(
            responseCode = "403",
            description = "No tiene permisos para listar órdenes",
            content = @Content(mediaType = "application/json")
        )
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'BUYER', 'WAREHOUSE', 'AUDITOR')")
    public ResponseEntity<PagedResponse<OrderResponse>> findAll(
            @Valid OrderFilterRequest filter) {
        return ResponseEntity.ok(orderService.findAll(filter));
    }

    @GetMapping("/{id}")
    @Operation(
        summary = "Obtener orden por ID",
        description = "Retorna el detalle completo de una orden de compra incluyendo líneas, proveedor y totales. " +
                      "Disponible para ADMIN, BUYER, WAREHOUSE y AUDITOR."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Orden encontrada",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = OrderResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Orden no encontrada",
            content = @Content(mediaType = "application/json")
        ),
        @ApiResponse(
            responseCode = "403",
            description = "No tiene permisos para ver esta orden",
            content = @Content(mediaType = "application/json")
        )
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'BUYER', 'WAREHOUSE', 'AUDITOR')")
    public ResponseEntity<OrderResponse> findById(
            @Parameter(description = "UUID de la orden", required = true)
            @PathVariable UUID id) {
        return ResponseEntity.ok(orderService.findById(id));
    }

    @PostMapping
    @Operation(
        summary = "Crear orden de compra",
        description = "Crea una nueva orden de compra con sus líneas. Valida stock, precios y proveedor. " +
                      "El estado inicial será PENDING. Disponible para ADMIN y BUYER."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "201",
            description = "Orden creada exitosamente",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = OrderResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Datos de la orden inválidos o proveedor/producto no existe",
            content = @Content(mediaType = "application/json")
        ),
        @ApiResponse(
            responseCode = "403",
            description = "No tiene permisos para crear órdenes",
            content = @Content(mediaType = "application/json")
        ),
        @ApiResponse(
            responseCode = "409",
            description = "Conflicto: SKU duplicado o orden ya existe",
            content = @Content(mediaType = "application/json")
        )
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'BUYER')")
    public ResponseEntity<OrderResponse> create(
            @Valid @RequestBody CreateOrderRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(orderService.create(request));
    }

    @PutMapping("/{id}/approve")
    @Operation(
        summary = "Aprobar orden de compra",
        description = "Aprueba una orden en estado PENDING. Dispara el proceso de compra y notificación al proveedor. " +
                      "Disponible para ADMIN y BUYER."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Orden aprobada exitosamente",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = OrderResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "La orden no puede ser aprobada (ya aprobada, cancelada o recibida)",
            content = @Content(mediaType = "application/json")
        ),
        @ApiResponse(
            responseCode = "403",
            description = "No tiene permisos para aprobar órdenes",
            content = @Content(mediaType = "application/json")
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Orden no encontrada",
            content = @Content(mediaType = "application/json")
        )
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'BUYER')")
    public ResponseEntity<OrderResponse> approve(
            @Parameter(description = "UUID de la orden a aprobar", required = true)
            @PathVariable UUID id) {
        return ResponseEntity.ok(orderService.approve(id));
    }

    @PutMapping("/{id}/receive")
    @Operation(
        summary = "Marcar orden como recibida",
        description = "Confirma la recepción de mercancía y actualiza el stock de los productos involucrados. " +
                      "La orden debe estar en estado APPROVED. Disponible para ADMIN y WAREHOUSE."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Orden recibida y stock actualizado",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = OrderResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "La orden no puede ser recibida (no está aprobada o ya fue recibida)",
            content = @Content(mediaType = "application/json")
        ),
        @ApiResponse(
            responseCode = "403",
            description = "No tiene permisos para recibir órdenes",
            content = @Content(mediaType = "application/json")
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Orden no encontrada",
            content = @Content(mediaType = "application/json")
        )
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'WAREHOUSE')")
    public ResponseEntity<OrderResponse> receive(
            @Parameter(description = "UUID de la orden recibida", required = true)
            @PathVariable UUID id) {
        return ResponseEntity.ok(orderService.receive(id));
    }

    @PutMapping("/{id}/cancel")
    @Operation(
        summary = "Cancelar orden de compra",
        description = "Cancela una orden en estado PENDING o APPROVED. No se puede cancelar si ya fue recibida. " +
                      "Disponible para ADMIN y BUYER."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Orden cancelada exitosamente",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = OrderResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "La orden no puede ser cancelada (ya recibida o previamente cancelada)",
            content = @Content(mediaType = "application/json")
        ),
        @ApiResponse(
            responseCode = "403",
            description = "No tiene permisos para cancelar órdenes",
            content = @Content(mediaType = "application/json")
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Orden no encontrada",
            content = @Content(mediaType = "application/json")
        )
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'BUYER')")
    public ResponseEntity<OrderResponse> cancel(
            @Parameter(description = "UUID de la orden a cancelar", required = true)
            @PathVariable UUID id) {
        return ResponseEntity.ok(orderService.cancel(id));
    }
}