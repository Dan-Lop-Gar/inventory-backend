package com.inventory.service;


import com.inventory.config.HazelcastConfig;
import com.inventory.entity.Supplier;
import com.inventory.repository.SupplierRepository;
import com.inventory.specification.SupplierSpecification;
import com.inventory.dto.CreateSupplierRequest;
import com.inventory.dto.SupplierFilterRequest;
import com.inventory.dto.PagedResponse;
import com.inventory.dto.SupplierResponse;
import com.inventory.exception.SupplierNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
public class SupplierService {

    private final SupplierRepository supplierRepository;

    public SupplierService(SupplierRepository supplierRepository) {
        this.supplierRepository = supplierRepository;
    }

    @Cacheable(value = HazelcastConfig.CACHE_SUPPLIERS,
            key = "'all-' + #filter.page() + '-' + #filter.size()")
    @Transactional(readOnly = true)
    public PagedResponse<SupplierResponse> findAll(SupplierFilterRequest filter) {
        Sort sort = buildSort(filter.sort());
        PageRequest pageable = PageRequest.of(filter.page(), filter.size(), sort);

        Specification<Supplier> spec = SupplierSpecification.withFilters(
                filter.name(),
                filter.country(),
                filter.active()
        );

        Page<SupplierResponse> page = supplierRepository
                .findAll(spec, pageable)
                .map(SupplierResponse::from);

        return PagedResponse.from(page);
    }

    @Cacheable(value = HazelcastConfig.CACHE_SUPPLIERS, key = "#id")
    @Transactional(readOnly = true)
    public SupplierResponse findById(UUID id) {
        return supplierRepository.findById(id)
                .filter(Supplier::getActive)
                .map(SupplierResponse::from)
                .orElseThrow(() -> new SupplierNotFoundException(id));
    }

    @CacheEvict(value = HazelcastConfig.CACHE_SUPPLIERS, allEntries = true)
    @Transactional
    public SupplierResponse create(CreateSupplierRequest request) {
        log.info("Creando proveedor: {}", request.name());

        boolean emailExists = supplierRepository
                .existsByEmailAndActiveTrue(request.email());

        if (emailExists) {
            throw new IllegalArgumentException(
                    "Ya existe un proveedor con el email: " + request.email()
            );
        }

        Supplier supplier = Supplier.builder()
                .name(request.name())
                .email(request.email())
                .phone(request.phone())
                .country(request.country())
                .address(request.address())
                .active(true)
                .build();

        return SupplierResponse.from(supplierRepository.save(supplier));
    }

    @CacheEvict(value = HazelcastConfig.CACHE_SUPPLIERS, allEntries = true)
    @Transactional
    public void delete(UUID id) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new SupplierNotFoundException(id));

        supplier.setActive(false);
        supplierRepository.save(supplier);
        log.info("Proveedor desactivado: {}", id);
    }

    private Sort buildSort(String sortParam) {
        if (sortParam == null || sortParam.isBlank()) {
            return Sort.by(Sort.Direction.ASC, "name");
        }
        String[] parts = sortParam.split(",");
        String field = parts[0].trim();
        Sort.Direction direction = parts.length > 1 &&
                parts[1].trim().equalsIgnoreCase("desc")
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;
        return Sort.by(direction, field);
    }
}
