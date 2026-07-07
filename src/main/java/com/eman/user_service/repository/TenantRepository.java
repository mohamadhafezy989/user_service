package com.eman.user_service.repository;

import com.eman.user_service.model.Tenant;
import com.eman.user_service.model.TenantStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TenantRepository extends JpaRepository<Tenant, UUID> {

    // ===== Basic Queries =====

    Optional<Tenant> findBySchemaName(String schemaName);

    Optional<Tenant> findByName(String name);

    List<Tenant> findByStatus(TenantStatus status);

    boolean existsBySchemaName(String schemaName);

    boolean existsByName(String name);

    // ===== Active Tenants =====

    @Query("SELECT t FROM Tenant t WHERE t.status = 'ACTIVE' AND (t.expiresAt IS NULL OR t.expiresAt > CURRENT_TIMESTAMP)")
    List<Tenant> findAllActiveTenants();

    @Query("SELECT t.schemaName FROM Tenant t WHERE t.status = 'ACTIVE' AND (t.expiresAt IS NULL OR t.expiresAt > CURRENT_TIMESTAMP)")
    List<String> findAllActiveSchemaNames();

    // ===== Update Operations =====

    @Modifying
    @Query("UPDATE Tenant t SET t.status = :status WHERE t.id = :tenantId")
    int updateStatus(@Param("tenantId") UUID tenantId, @Param("status") TenantStatus status);

    @Modifying
    @Query("UPDATE Tenant t SET t.subscriptionPlan = :plan WHERE t.id = :tenantId")
    int updateSubscriptionPlan(@Param("tenantId") UUID tenantId,
                               @Param("plan") String plan);

    // ===== Expired Tenants =====

    @Query("SELECT t FROM Tenant t WHERE t.expiresAt IS NOT NULL AND t.expiresAt < CURRENT_TIMESTAMP")
    List<Tenant> findExpiredTenants();

    @Modifying
    @Query("UPDATE Tenant t SET t.status = 'SUSPENDED' WHERE t.expiresAt IS NOT NULL AND t.expiresAt < CURRENT_TIMESTAMP")
    int suspendExpiredTenants();

    // ===== Statistics =====

    @Query("SELECT t.status, COUNT(t) FROM Tenant t GROUP BY t.status")
    List<Object[]> countByStatus();

    @Query("SELECT t.subscriptionPlan, COUNT(t) FROM Tenant t GROUP BY t.subscriptionPlan")
    List<Object[]> countBySubscriptionPlan();

    // ===== Search =====

    @Query("SELECT t FROM Tenant t WHERE LOWER(t.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
            "OR LOWER(t.displayName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<Tenant> searchTenants(@Param("searchTerm") String searchTerm);
}
