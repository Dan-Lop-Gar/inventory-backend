package com.inventory.dto;

import com.inventory.enums.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Public data of order filer request")
public record OrderFilterRequest(
    @Schema(example = "STATUS")
    OrderStatus status,

    @Schema(example = "334e05da-d1e2-476f-9fb1-90d555cdaf69")
    UUID supplierId,

    @Schema(example = "user")
    String createdBy,

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @Schema(example = "11-03-02 08:32:34")
    LocalDateTime dateFrom,

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @Schema(example = "15-03-02 08:32:34")
    LocalDateTime dateTo,

    @Min(0)
    @Schema(example = "1")
    Integer page,

    @Min(1)
    @Max(100)
    @Schema(example = "10")
    Integer size,

    @Schema(example = "createdAt,desc")
    String sort
) {
    public OrderFilterRequest {
        if (page == null) page = 0;
        if (size == null) size = 20;
        if (sort == null) sort = "createdAt,desc";
    }
}
