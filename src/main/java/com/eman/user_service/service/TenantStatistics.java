package com.eman.user_service.service;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * Statistics about tenants
 */
@Data
@Builder
public class TenantStatistics {
    private long totalTenants;
    private Map<String, Long> tenantsByStatus;
    private Map<String, Long> tenantsBySubscription;
}

