package com.eman.user_service.tenancy;


import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filter that ensures tenant context is set before request processing
 * Alternative to interceptor with earlier execution order
 */
@Slf4j
@Component
@Order(1)
public class TenantAwareFilter extends OncePerRequestFilter {

    @Value("${taskmanager.tenancy.tenant-header-name:X-Tenant-ID}")
    private String tenantHeaderName;

    @Value("${taskmanager.tenancy.default-tenant-schema-prefix:tenant_}")
    private String schemaPrefix;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();
        String tenantId = request.getHeader(tenantHeaderName);
        // Check for public paths
        if (isPublicPath(path)) {
            if (tenantId != null) {
                String schemaName = schemaPrefix + tenantId.toLowerCase().trim();
                TenantContext.setTenant(schemaName);
                TenantContext.setTenantId(tenantId);
                org.slf4j.MDC.put("tenant", tenantId);
            }
            filterChain.doFilter(request, response);
            return;
        }



        if (tenantId == null || tenantId.isBlank()) {
            log.warn("Missing tenant header for path: {}", path);
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\": \"Tenant ID is required\"}");
            return;
        }

        try {
            String schemaName = schemaPrefix + tenantId.toLowerCase().trim();
            TenantContext.setTenant(schemaName);
            TenantContext.setTenantId(tenantId);
            org.slf4j.MDC.put("tenant", tenantId);

            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
            org.slf4j.MDC.remove("tenant");
        }
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
}