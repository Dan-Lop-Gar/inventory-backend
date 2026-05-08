package com.inventory.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

@Schema(description = "Public data of create order request")
public record CreateOrderRequest(
    @NotNull(message = "Proveedor es requerido")
    @Schema(example = "334e05da-d1e2-476f-9fb1-90d555cdaf69")
    UUID supplierId,
    @Schema(example = "order note")
    String notes,
    @NotEmpty(message = "La orden debe tener al menos una línea")
    @Valid
    @Schema(
        example =
                    """
                        {
                            {
                                productId: 334e05da-d1e2-476f-9fb1-90d555cdaf69
                                quantity: 56
                                unitPrice: 22.35
                            }
                        }
                    """
    )
    List<OrderLineRequest> lines
) {}
