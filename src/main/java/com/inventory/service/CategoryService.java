package com.inventory.service;


import com.inventory.config.HazelcastConfig;
import com.inventory.entity.Category;
import com.inventory.repository.CategoryRepository;
import com.inventory.dto.CategoryFilterRequest;
import com.inventory.dto.CreateCategoryRequest;
import com.inventory.dto.CategoryResponse;
import com.inventory.dto.PagedResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Cacheable(value = HazelcastConfig.CACHE_CATEGORIES, key = "'all-list'")
    @Transactional(readOnly = true)
    public List<CategoryResponse> findAll() {
        return categoryRepository.findAllOrderedByName()
                .stream()
                .map(CategoryResponse::from)
                .toList();
    }

    @Cacheable(
            value = HazelcastConfig.CACHE_CATEGORIES,
            key = "'paged-' + #filter.page() + '-' + #filter.size()"
    )
    @Transactional(readOnly = true)
    public PagedResponse<CategoryResponse> findPaged(CategoryFilterRequest filter) {
        Sort sort = buildSort(filter.sort());
        PageRequest pageable = PageRequest.of(filter.page(), filter.size(), sort);

        Page<CategoryResponse> page = categoryRepository.findAll(pageable)
                .map(CategoryResponse::from);

        return PagedResponse.from(page);
    }

    @Cacheable(value = HazelcastConfig.CACHE_CATEGORIES, key = "#id")
    @Transactional(readOnly = true)
    public CategoryResponse findById(UUID id) {
        return categoryRepository.findById(id)
                .map(CategoryResponse::from)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Categoría no encontrada: " + id
                ));
    }

    @CacheEvict(value = HazelcastConfig.CACHE_CATEGORIES, allEntries = true)
    @Transactional
    public CategoryResponse create(CreateCategoryRequest request) {
        log.info("Creando categoría: {}", request.name());

        if (categoryRepository.existsByNameIgnoreCase(request.name())) {
            throw new IllegalArgumentException(
                    "Ya existe una categoría con el nombre: " + request.name()
            );
        }

        Category category = Category.builder()
                .name(request.name())
                .description(request.description())
                .build();

        return CategoryResponse.from(categoryRepository.save(category));
    }

    @CacheEvict(value = HazelcastConfig.CACHE_CATEGORIES, allEntries = true)
    @Transactional
    public CategoryResponse update(UUID id, CreateCategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Categoría no encontrada: " + id
                ));

        boolean nameExists = categoryRepository.existsByNameIgnoreCase(request.name())
                && !category.getName().equalsIgnoreCase(request.name());

        if (nameExists) {
            throw new IllegalArgumentException(
                    "Ya existe una categoría con el nombre: " + request.name()
            );
        }

        category.setName(request.name());
        category.setDescription(request.description());

        return CategoryResponse.from(categoryRepository.save(category));
    }

    @CacheEvict(value = HazelcastConfig.CACHE_CATEGORIES, allEntries = true)
    @Transactional
    public void delete(UUID id) {
        if (!categoryRepository.existsById(id)) {
            throw new IllegalArgumentException("Categoría no encontrada: " + id);
        }
        categoryRepository.deleteById(id);
        log.info("Categoría eliminada: {}", id);
    }

    private Sort buildSort(String sortParam) {
        if (sortParam == null || sortParam.isBlank()) {
            return Sort.by(Sort.Direction.ASC, "name");
        }
        String[] parts = sortParam.split(",");
        String field = parts[0].trim();
        Sort.Direction dir = parts.length > 1 &&
                parts[1].trim().equalsIgnoreCase("desc")
                ? Sort.Direction.DESC : Sort.Direction.ASC;
        return Sort.by(dir, field);
    }
}