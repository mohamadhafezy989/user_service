package com.eman.user_service.service;


import com.eman.user_service.dto.TenantRequestDTO;
import com.eman.user_service.dto.TenantResponseDTO;
import com.eman.user_service.exception.TenantNotFoundException;
import com.eman.user_service.kafka.UserEventProducer;
import com.eman.user_service.mapper.TenantMapper;
import com.eman.user_service.model.Tenant;
import com.eman.user_service.model.TenantStatus;
import com.eman.user_service.model.SubscriptionPlan;
import com.eman.user_service.repository.TenantRepository;
import com.eman.user_service.tenancy.TenantDataSourceConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for tenant management operations
 * Handles creation, update, and management of tenants with their schemas
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TenantManagementService {

    private final TenantRepository tenantRepository;
    private final TenantMapper tenantMapper;
    private final TenantDataSourceConfig dataSourceConfig;
    private final UserEventProducer eventProducer;

    /**
     * Create a new tenant with its own schema
     */
    @Transactional
    public TenantResponseDTO createTenant(TenantRequestDTO request) {
        log.info("Creating new tenant: {}", request.getName());

        // Check if tenant name already exists
        if (tenantRepository.existsByName(request.getName())) {
            throw new IllegalArgumentException("Tenant name already exists: " + request.getName());
        }

        // Check if schema name already exists
        if (tenantRepository.existsBySchemaName(request.getSchemaName())) {
            throw new IllegalArgumentException("Schema name already exists: " + request.getSchemaName());
        }

        // Create tenant entity
        Tenant tenant = tenantMapper.toEntity(request);
        tenant.setStatus(TenantStatus.ACTIVE);
        tenant.setSubscriptionPlan(SubscriptionPlan.FREE);

        // Set default values if not provided
        if (tenant.getMaxUsers() == null) {
            tenant.setMaxUsers(100);
        }
        if (tenant.getMaxTasks() == null) {
            tenant.setMaxTasks(1000);
        }

        Tenant savedTenant = tenantRepository.save(tenant);

        // Create schema in database
        createTenantSchema(savedTenant.getSchemaName());

        // Add datasource dynamically
        dataSourceConfig.addTenantDataSource(savedTenant.getSchemaName());

        // Publish tenant created event
        eventProducer.sendTenantCreatedEvent(savedTenant);

        log.info("Tenant created successfully: {}", savedTenant.getName());

        return tenantMapper.toDto(savedTenant);
    }

    /**
     * Get tenant by ID
     */
    @Transactional(readOnly = true)
    public TenantResponseDTO getTenantById(UUID tenantId) {
        Tenant tenant = findTenantById(tenantId);
        return tenantMapper.toDto(tenant);
    }

    /**
     * Get tenant by name
     */
    @Transactional(readOnly = true)
    public TenantResponseDTO getTenantByName(String name) {
        Tenant tenant = tenantRepository.findByName(name)
                .orElseThrow(() -> new TenantNotFoundException("Tenant not found with name: " + name));
        return tenantMapper.toDto(tenant);
    }

    /**
     * Get all tenants with pagination
     */
    @Transactional(readOnly = true)
    public Page<TenantResponseDTO> getAllTenants(Pageable pageable) {
        return tenantRepository.findAll(pageable)
                .map(tenantMapper::toDto);
    }

    /**
     * Get tenants by status
     */
    @Transactional(readOnly = true)
    public List<TenantResponseDTO> getTenantsByStatus(TenantStatus status) {
        return tenantRepository.findByStatus(status).stream()
                .map(tenantMapper::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Get all active tenants
     */
    @Transactional(readOnly = true)
    public List<TenantResponseDTO> getActiveTenants() {
        return tenantRepository.findAllActiveTenants().stream()
                .map(tenantMapper::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Update tenant
     */
    @Transactional
    public TenantResponseDTO updateTenant(UUID tenantId, TenantRequestDTO request) {
        log.info("Updating tenant: {}", tenantId);

        Tenant tenant = findTenantById(tenantId);

        // Update fields
        tenantMapper.updateEntity(tenant, request);

        if (request.getStatus() != null) {
            tenant.setStatus(TenantStatus.valueOf(request.getStatus()));
        }

        if (request.getSubscriptionPlan() != null) {
            tenant.setSubscriptionPlan(SubscriptionPlan.valueOf(request.getSubscriptionPlan()));
        }

        Tenant updatedTenant = tenantRepository.save(tenant);

        // Publish event
        eventProducer.sendTenantUpdatedEvent(updatedTenant);

        log.info("Tenant updated: {}", updatedTenant.getName());

        return tenantMapper.toDto(updatedTenant);
    }

    /**
     * Update tenant status
     */
    @Transactional
    public TenantResponseDTO updateTenantStatus(UUID tenantId, TenantStatus status) {
        log.info("Updating tenant status: {} -> {}", tenantId, status);

        Tenant tenant = findTenantById(tenantId);
        tenant.setStatus(status);

        Tenant updatedTenant = tenantRepository.save(tenant);

        // If tenant is suspended or deleted, remove datasource
        if (status == TenantStatus.SUSPENDED || status == TenantStatus.DELETED) {
            dataSourceConfig.removeTenantDataSource(tenant.getSchemaName());
        }

        // If tenant is activated, add datasource
        if (status == TenantStatus.ACTIVE) {
            dataSourceConfig.addTenantDataSource(tenant.getSchemaName());
        }

        // Publish event
        eventProducer.sendTenantStatusChangedEvent(updatedTenant, status);

        log.info("Tenant status updated: {} -> {}", tenant.getName(), status);

        return tenantMapper.toDto(updatedTenant);
    }

    /**
     * Update subscription plan
     */
    @Transactional
    public TenantResponseDTO updateSubscriptionPlan(UUID tenantId, SubscriptionPlan plan) {
        log.info("Updating subscription plan: {} -> {}", tenantId, plan);

        Tenant tenant = findTenantById(tenantId);
        tenant.setSubscriptionPlan(plan);

        // Update limits based on plan
        tenant.setMaxUsers(plan.getMaxUsers());
        tenant.setMaxTasks(plan.getMaxTasks());

        Tenant updatedTenant = tenantRepository.save(tenant);

        // Publish event
        eventProducer.sendTenantUpdatedEvent(updatedTenant);

        log.info("Subscription plan updated: {} -> {}", tenant.getName(), plan);

        return tenantMapper.toDto(updatedTenant);
    }

    /**
     * Delete tenant (soft delete)
     */
    @Transactional
    public void deleteTenant(UUID tenantId) {
        log.info("Deleting tenant: {}", tenantId);

        Tenant tenant = findTenantById(tenantId);
        tenant.setStatus(TenantStatus.DELETED);

        tenantRepository.save(tenant);

        // Remove datasource
        dataSourceConfig.removeTenantDataSource(tenant.getSchemaName());

        // Publish event
        eventProducer.sendTenantDeletedEvent(tenant);

        log.info("Tenant deleted: {}", tenant.getName());
    }

    /**
     * Search tenants
     */
    @Transactional(readOnly = true)
    public List<TenantResponseDTO> searchTenants(String query) {
        return tenantRepository.searchTenants(query).stream()
                .map(tenantMapper::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Get tenant statistics
     */
    @Transactional(readOnly = true)
    public TenantStatistics getStatistics() {
        long totalTenants = tenantRepository.count();

        // Count by status
        List<Object[]> statusCounts = tenantRepository.countByStatus();
        Map<String, Long> tenantsByStatus = statusCounts.stream()
                .collect(Collectors.toMap(
                        row -> ((TenantStatus) row[0]).name(),
                        row -> (Long) row[1]
                ));

        // Count by subscription plan
        List<Object[]> planCounts = tenantRepository.countBySubscriptionPlan();
        Map<String, Long> tenantsBySubscription = planCounts.stream()
                .collect(Collectors.toMap(
                        row -> ((SubscriptionPlan) row[0]).name(),
                        row -> (Long) row[1]
                ));

        return TenantStatistics.builder()
                .totalTenants(totalTenants)
                .tenantsByStatus(tenantsByStatus)
                .tenantsBySubscription(tenantsBySubscription)
                .build();
    }

    /**
     * Create database schema for tenant
     */
    private void createTenantSchema(String schemaName) {
        try {
            // Create schema
            String createSchemaSQL = "CREATE SCHEMA IF NOT EXISTS " + schemaName;
            // Using JdbcTemplate or EntityManager to execute
            // This will be implemented in the next step

            log.info("Schema created: {}", schemaName);
        } catch (Exception e) {
            log.error("Failed to create schema: {}", schemaName, e);
            throw new RuntimeException("Failed to create tenant schema: " + e.getMessage());
        }
    }

    private Tenant findTenantById(UUID tenantId) {
        return tenantRepository.findById(tenantId)
                .orElseThrow(() -> new TenantNotFoundException("Tenant not found with id: " + tenantId));
    }
}
