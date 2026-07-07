package com.eman.user_service.model;


import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tenants", schema = "shared")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tenant {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(name = "schema_name", nullable = false, unique = true, length = 100)
    private String schemaName;

    @Column(name = "display_name", length = 200)
    private String displayName;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private TenantStatus status = TenantStatus.ACTIVE;
    @JdbcTypeCode(SqlTypes.JSON)
    private String config;

    @Column(name = "max_users")
    @Builder.Default
    private Integer maxUsers = 100;

    @Column(name = "max_tasks")
    @Builder.Default
    private Integer maxTasks = 1000;

    @Column(name = "subscription_plan")
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private SubscriptionPlan subscriptionPlan = SubscriptionPlan.FREE;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Transient
    public boolean isActive() {
        return status == TenantStatus.ACTIVE;
    }

    @Transient
    public boolean isExpired() {
        if (expiresAt == null) {
            return false;
        }
        return expiresAt.isBefore(LocalDateTime.now());
    }

    @Transient
    public boolean canAddUser() {
        // In real implementation, count users from schema
        return true;
    }
}
