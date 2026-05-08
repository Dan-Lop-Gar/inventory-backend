package com.inventory.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Public data of a createSupplierRequest")
public record CreateSupplierRequest(
    @NotBlank(message = "Nombre es requerido")
    @Size(max = 200)
    @Schema(example = "sup-1")
    String name,

    @NotBlank(message = "Email es requerido")
    @Email(message = "Email inválido")
    @Size(max = 255)
    @Schema(example = "sup-1@example.com")
    String email,

    @Size(max = 20)
    @Schema(example = "5512345678")
    String phone,

    @Size(max = 100)
    @Schema(example = "US")
    String country,

    @Schema(example = "Escuadron Street")
    String address
) {}