package com.eman.user_service.dto;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Tenant request DTO for create/update operations
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantRequestDTO {

    @NotBlank(message = "Tenant name is required")
    private String name;

    @NotBlank(message = "Schema name is required")
    private String schemaName;

    private String displayName;

    private String status;

    private String config;

    private Integer maxUsers;

    private Integer maxTasks;

    private String subscriptionPlan;

    private LocalDateTime expiresAt;
}