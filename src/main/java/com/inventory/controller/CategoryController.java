package com.inventory.controller;

import com.inventory.dto.CategoryFilterRequest;
import com.inventory.dto.CreateCategoryRequest;
import com.inventory.dto.CategoryResponse;
import com.inventory.dto.PagedResponse;
import com.inventory.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
@Tag(name = "Categories", description = "Gestión de categorías de productos")
@SecurityRequirement(name = "bearerAuth")
public class CategoryController {

    private final CategoryService categoryService;

    // Para los selects del frontend — devuelve lista completa sin paginar
    @GetMapping
    @Operation(summary = "Listar todas las categorías")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<CategoryResponse>> findAll() {
        return ResponseEntity.ok(categoryService.findAll());
    }

    @GetMapping("/paged")
    @Operation(summary = "Listar categorías con paginación")
    @PreAuthorize("hasAnyRole('ADMIN', 'BUYER', 'AUDITOR')")
    public ResponseEntity<PagedResponse<CategoryResponse>> findPaged(
            @Valid CategoryFilterRequest filter) {
        return ResponseEntity.ok(categoryService.findPaged(filter));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener categoría por ID")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CategoryResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(categoryService.findById(id));
    }

    @PostMapping
    @Operation(summary = "Crear nueva categoría")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CategoryResponse> create(
            @Valid @RequestBody CreateCategoryRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(categoryService.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar categoría")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CategoryResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody CreateCategoryRequest request) {
        return ResponseEntity.ok(categoryService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar categoría")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        categoryService.delete(id);
        return ResponseEntity.noContent().build();
    }
}