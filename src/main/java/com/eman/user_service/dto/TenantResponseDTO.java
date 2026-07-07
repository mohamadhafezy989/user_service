package com.eman.user_service.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Tenant response DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantResponseDTO {

    private UUID id;
    private String name;
    private String schemaName;
    private String displayName;
    private String status;
    private String config;
    private Integer maxUsers;
    private Integer maxTasks;
    private String subscriptionPlan;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime expiresAt;
}
