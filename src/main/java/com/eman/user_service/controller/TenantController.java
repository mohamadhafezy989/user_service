package com.eman.user_service.controller;


import com.eman.user_service.dto.TenantRequestDTO;
import com.eman.user_service.dto.TenantResponseDTO;
import com.eman.user_service.model.TenantStatus;
import com.eman.user_service.model.SubscriptionPlan;
import com.eman.user_service.service.TenantManagementService;
import com.eman.user_service.service.TenantStatistics;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controller for tenant management (admin only)
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/tenants")
@RequiredArgsConstructor
@Tag(name = "Tenant Management", description = "APIs for managing tenants (Admin only)")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class TenantController {

    private final TenantManagementService tenantManagementService;

    @Operation(summary = "Create a new tenant", description = "Creates a new tenant with its own schema")
    @PostMapping
    public ResponseEntity<TenantResponseDTO> createTenant(@Valid @RequestBody TenantRequestDTO request) {
        log.info("Creating new tenant: {}", request.getName());
        TenantResponseDTO tenant = tenantManagementService.createTenant(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(tenant);
    }

    @Operation(summary = "Get tenant by ID", description = "Returns tenant details")
    @GetMapping("/{tenantId}")
    public ResponseEntity<TenantResponseDTO> getTenantById(@PathVariable UUID tenantId) {
        log.info("Getting tenant by ID: {}", tenantId);
        TenantResponseDTO tenant = tenantManagementService.getTenantById(tenantId);
        return ResponseEntity.ok(tenant);
    }

    @Operation(summary = "Get tenant by name", description = "Returns tenant details by logical name")
    @GetMapping("/name/{name}")
    public ResponseEntity<TenantResponseDTO> getTenantByName(@PathVariable String name) {
        log.info("Getting tenant by name: {}", name);
        TenantResponseDTO tenant = tenantManagementService.getTenantByName(name);
        return ResponseEntity.ok(tenant);
    }

    @Operation(summary = "Get all tenants", description = "Returns a paginated list of all tenants")
    @GetMapping
    public ResponseEntity<Page<TenantResponseDTO>> getAllTenants(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        log.info("Getting all tenants");
        Page<TenantResponseDTO> tenants = tenantManagementService.getAllTenants(pageable);
        return ResponseEntity.ok(tenants);
    }

    @Operation(summary = "Get tenants by status", description = "Returns tenants with the specified status")
    @GetMapping("/status/{status}")
    public ResponseEntity<List<TenantResponseDTO>> getTenantsByStatus(@PathVariable TenantStatus status) {
        log.info("Getting tenants by status: {}", status);
        List<TenantResponseDTO> tenants = tenantManagementService.getTenantsByStatus(status);
        return ResponseEntity.ok(tenants);
    }

    @Operation(summary = "Get all active tenants", description = "Returns all active tenants")
    @GetMapping("/active")
    public ResponseEntity<List<TenantResponseDTO>> getActiveTenants() {
        log.info("Getting all active tenants");
        List<TenantResponseDTO> tenants = tenantManagementService.getActiveTenants();
        return ResponseEntity.ok(tenants);
    }

    @Operation(summary = "Update tenant", description = "Updates tenant details")
    @PutMapping("/{tenantId}")
    public ResponseEntity<TenantResponseDTO> updateTenant(
            @PathVariable UUID tenantId,
            @Valid @RequestBody TenantRequestDTO request) {
        log.info("Updating tenant: {}", tenantId);
        TenantResponseDTO updated = tenantManagementService.updateTenant(tenantId, request);
        return ResponseEntity.ok(updated);
    }

    @Operation(summary = "Update tenant status", description = "Activates, suspends, or deletes a tenant")
    @PatchMapping("/{tenantId}/status")
    public ResponseEntity<TenantResponseDTO> updateTenantStatus(
            @PathVariable UUID tenantId,
            @RequestParam TenantStatus status) {
        log.info("Updating tenant status: {} -> {}", tenantId, status);
        TenantResponseDTO updated = tenantManagementService.updateTenantStatus(tenantId, status);
        return ResponseEntity.ok(updated);
    }

    @Operation(summary = "Update subscription plan", description = "Updates tenant subscription plan")
    @PatchMapping("/{tenantId}/subscription")
    public ResponseEntity<TenantResponseDTO> updateSubscription(
            @PathVariable UUID tenantId,
            @RequestParam SubscriptionPlan plan) {
        log.info("Updating tenant subscription: {} -> {}", tenantId, plan);
        TenantResponseDTO updated = tenantManagementService.updateSubscriptionPlan(tenantId, plan);
        return ResponseEntity.ok(updated);
    }

    @Operation(summary = "Delete tenant", description = "Deletes a tenant (soft delete)")
    @DeleteMapping("/{tenantId}")
    public ResponseEntity<Void> deleteTenant(@PathVariable UUID tenantId) {
        log.info("Deleting tenant: {}", tenantId);
        tenantManagementService.deleteTenant(tenantId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Search tenants", description = "Search tenants by name or display name")
    @GetMapping("/search")
    public ResponseEntity<List<TenantResponseDTO>> searchTenants(@RequestParam String query) {
        log.info("Searching tenants: {}", query);
        List<TenantResponseDTO> tenants = tenantManagementService.searchTenants(query);
        return ResponseEntity.ok(tenants);
    }

    @Operation(summary = "Get tenant statistics", description = "Returns statistics about tenants")
    @GetMapping("/statistics")
    public ResponseEntity<TenantStatistics> getStatistics() {
        log.info("Getting tenant statistics");
        TenantStatistics stats = tenantManagementService.getStatistics();
        return ResponseEntity.ok(stats);
    }
}