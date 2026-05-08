package com.inventory.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record CategoryFilterRequest(
        String name,

        @Min(0) Integer page,
        @Min(1) @Max(100) Integer size,
        String sort
) {
    public CategoryFilterRequest {
        if (page == null) page = 0;
        if (size == null) size = 20;
        if (sort == null) sort = "name,asc";
    }
}
