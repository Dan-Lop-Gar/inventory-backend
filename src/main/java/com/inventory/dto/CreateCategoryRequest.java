package com.inventory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCategoryRequest(
        @NotBlank(message = "Nombre es requerido")
        @Size(max = 100, message = "Nombre máximo 100 caracteres")
        String name,

        String description
) {}
