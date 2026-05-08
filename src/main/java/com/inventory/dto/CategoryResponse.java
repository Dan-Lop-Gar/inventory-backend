package com.inventory.dto;

import com.inventory.entity.Category;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

public record CategoryResponse(
        UUID id,
        String name,
        String description,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) implements Serializable {
    private static final long serialVersionUID = 1L;

    public static CategoryResponse from(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getDescription(),
                category.getCreatedAt(),
                category.getUpdatedAt()
        );
    }
}
