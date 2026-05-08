package com.inventory.controller;

import com.inventory.dto.CreateSupplierRequest;
import com.inventory.dto.PagedResponse;
import com.inventory.dto.SupplierFilterRequest;
import com.inventory.dto.SupplierResponse;
import com.inventory.service.SupplierService;
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
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/suppliers")
@RequiredArgsConstructor
@Tag(name = "Suppliers", description = "Gestión de proveedores del sistema de inventario")
@SecurityRequirement(name = "bearerAuth")
public class SuppliersController {

    private final SupplierService supplierService;

    @GetMapping
    @Operation(
        summary = "Listar proveedores",
        description = """
            Retorna una página paginada de proveedores. Todos los filtros son opcionales y se combinan con AND.
            
            **Filtros disponibles:**
            - `name` — contiene el texto (ej: `Acme`)
            - `email` — contiene el texto (ej: `acme@corp.com`)
            - `phone` — contiene el texto (ej: `555`)
            - `country` — país exacto (ej: `México`)
            - `active` — filtra por estado activo/inactivo (default: `true`)
            
            **Paginación:**
            - `page` — número de página, base 0 (default: `0`)
            - `size` — tamaño de página, máximo 100 (default: `20`)
            
            **Ordenamiento:**
            - `sort` — campo y dirección separados por coma (default: `name,asc`)
            - Campos disponibles: `id | name | email | country | createdAt`
            - Direcciones: `asc | desc`
            
            **Ejemplos:**
                GET /api/suppliers?name=Acme&page=0&size=10
                GET /api/suppliers?country=México&active=true&sort=createdAt,desc
                GET /api/suppliers?email=corp.com&page=0&size=50
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Lista de proveedores obtenida exitosamente",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = PagedResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Parámetros de filtro inválidos (página negativa, tamaño excedido, etc.)",
            content = @Content(mediaType = "application/json")
        ),
        @ApiResponse(
            responseCode = "403",
            description = "No tiene permisos para listar proveedores. Requiere rol ADMIN, BUYER o AUDITOR",
            content = @Content(mediaType = "application/json")
        )
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'BUYER', 'AUDITOR')")
    public ResponseEntity<PagedResponse<SupplierResponse>> findAll(
            @Valid SupplierFilterRequest filter) {
        return ResponseEntity.ok(supplierService.findAll(filter));
    }

    @GetMapping("/{id}")
    @Operation(
        summary = "Obtener proveedor por ID",
        description = "Retorna el detalle completo de un proveedor incluyendo datos de contacto, país y estado. " +
                      "Disponible para ADMIN, BUYER y AUDITOR."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Proveedor encontrado",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = SupplierResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Proveedor no encontrado con el ID proporcionado",
            content = @Content(mediaType = "application/json")
        ),
        @ApiResponse(
            responseCode = "403",
            description = "No tiene permisos para ver proveedores. Requiere rol ADMIN, BUYER o AUDITOR",
            content = @Content(mediaType = "application/json")
        )
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'BUYER', 'AUDITOR')")
    public ResponseEntity<SupplierResponse> findById(
            @Parameter(description = "UUID del proveedor", required = true)
            @PathVariable UUID id) {
        return ResponseEntity.ok(supplierService.findById(id));
    }

    @PostMapping
    @Operation(
        summary = "Crear proveedor",
        description = "Crea un nuevo proveedor en el sistema. Valida que el email sea único y que el nombre no esté vacío. " +
                      "El proveedor se crea con estado activo por defecto. Disponible para ADMIN y BUYER."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "201",
            description = "Proveedor creado exitosamente",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = SupplierResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Datos del proveedor inválidos (email duplicado, nombre vacío, email mal formado, etc.)",
            content = @Content(mediaType = "application/json")
        ),
        @ApiResponse(
            responseCode = "403",
            description = "No tiene permisos para crear proveedores. Requiere rol ADMIN o BUYER",
            content = @Content(mediaType = "application/json")
        ),
        @ApiResponse(
            responseCode = "409",
            description = "Conflicto: ya existe un proveedor con el mismo email",
            content = @Content(mediaType = "application/json")
        )
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'BUYER')")
    public ResponseEntity<SupplierResponse> create(
            @Valid @RequestBody CreateSupplierRequest request) {
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(supplierService.create(request));
    }

    @DeleteMapping("/{id}")
    @Operation(
        summary = "Desactivar proveedor",
        description = "Desactiva un proveedor existente (soft delete). El proveedor no se elimina de la base de datos, " +
                      "sino que se marca como inactivo. No se pueden crear órdenes con proveedores inactivos. " +
                      "Solo disponible para ADMIN."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "204",
            description = "Proveedor desactivado exitosamente (sin contenido en respuesta)",
            content = @Content(mediaType = "application/json")
        ),
        @ApiResponse(
            responseCode = "403",
            description = "No tiene permisos para desactivar proveedores. Requiere rol ADMIN",
            content = @Content(mediaType = "application/json")
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Proveedor no encontrado con el ID proporcionado",
            content = @Content(mediaType = "application/json")
        ),
        @ApiResponse(
            responseCode = "409",
            description = "No se puede desactivar: el proveedor tiene órdenes activas asociadas",
            content = @Content(mediaType = "application/json")
        )
    })
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(
            @Parameter(description = "UUID del proveedor a desactivar", required = true)
            @PathVariable UUID id) {
        supplierService.delete(id);
        return ResponseEntity.noContent().build();
    }
}