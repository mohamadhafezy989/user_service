package com.eman.user_service.tenancy;


import lombok.extern.slf4j.Slf4j;

/**
 * Thread-local storage for current tenant context
 * Holds tenant schema name and tenant ID for the current request
 */
@Slf4j
public final class TenantContext {

    private static final ThreadLocal<String> currentTenant = new ThreadLocal<>();
    private static final ThreadLocal<String> currentTenantId = new ThreadLocal<>();

    private TenantContext() {
        // Prevent instantiation
    }

    /**
     * Set the current tenant schema
     */
    public static void setTenant(String schemaName) {
        log.debug("Setting tenant schema: {}", schemaName);
        currentTenant.set(schemaName);
    }

    /**
     * Get the current tenant schema
     */
    public static String getTenant() {
        return currentTenant.get();
    }

    /**
     * Set the current tenant ID (logical name)
     */
    public static void setTenantId(String tenantId) {
        log.debug("Setting tenant ID: {}", tenantId);
        currentTenantId.set(tenantId);
    }

    /**
     * Get the current tenant ID
     */
    public static String getTenantId() {
        return currentTenantId.get();
    }

    /**
     * Clear the tenant context (called after request completion)
     */
    public static void clear() {
        log.debug("Clearing tenant context");
        currentTenant.remove();
        currentTenantId.remove();
    }

    /**
     * Check if tenant is set
     */
    public static boolean isTenantSet() {
        return currentTenant.get() != null;
    }

    /**
     * Get required tenant or throw exception
     */
    public static String getRequiredTenant() {
        String tenant = getTenant();
        if (tenant == null) {
            throw new IllegalStateException("Tenant schema not set in current context");
        }
        return tenant;
    }

    /**
     * Get required tenant ID or throw exception
     */
    public static String getRequiredTenantId() {
        String tenantId = getTenantId();
        if (tenantId == null) {
            throw new IllegalStateException("Tenant ID not set in current context");
        }
        return tenantId;
    }
}

