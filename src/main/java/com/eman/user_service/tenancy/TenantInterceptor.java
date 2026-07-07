package com.eman.user_service.tenancy;


import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Interceptor that extracts tenant ID from request header
 * and sets it in the TenantContext
 */
@Slf4j
@Component
public class TenantInterceptor implements HandlerInterceptor {

    @Value("${taskmanager.tenancy.tenant-header-name:X-Tenant-ID}")
    private String tenantHeaderName;

    @Value("${taskmanager.tenancy.default-tenant-schema-prefix:tenant_}")
    private String schemaPrefix;

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {

        // Extract tenant ID from header
        String tenantId = request.getHeader(tenantHeaderName);

        // Public endpoints that don't require tenant
        if (isPublicPath(request.getRequestURI())) {
            return true;
        }

        if (tenantId == null || tenantId.isBlank()) {
            log.warn("No tenant header found in request: {}", request.getRequestURI());
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\": \"Tenant ID is required in header: " + tenantHeaderName + "\"}");
            return false;
        }

        // Convert tenant ID to schema name (e.g., acme -> tenant_acme)
        String schemaName = convertTenantIdToSchemaName(tenantId);

        // Set in ThreadLocal
        TenantContext.setTenant(schemaName);
        TenantContext.setTenantId(tenantId);

        // Add to MDC for logging
        org.slf4j.MDC.put("tenant", tenantId);

        log.debug("Tenant set: {} -> schema: {}", tenantId, schemaName);

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response,
                                Object handler,
                                Exception ex) throws Exception {
        // Clear ThreadLocal and MDC
        TenantContext.clear();
        org.slf4j.MDC.remove("tenant");
        log.debug("Tenant context cleared");
    }

    private boolean isPublicPath(String path) {
        return path.startsWith("/actuator") ||
                path.startsWith("/health") ||
                path.startsWith("/metrics") ||
                path.startsWith("/swagger-ui") ||
                path.startsWith("/v3/api-docs") ||
                path.equals("/api/auth/register") ||
                path.equals("/api/auth/login") ||
                path.equals("/api/auth/refresh") ||
                path.equals("/api/public");
    }

    private String convertTenantIdToSchemaName(String tenantId) {
        // Sanitize tenant ID for schema name
        String safeTenantId = tenantId.toLowerCase().trim()
                .replaceAll("[^a-z0-9_]", "_");
        return schemaPrefix + safeTenantId;
    }
}

