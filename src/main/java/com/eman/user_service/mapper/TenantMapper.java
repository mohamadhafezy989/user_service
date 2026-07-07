package com.eman.user_service.mapper;


import com.eman.user_service.dto.TenantRequestDTO;
import com.eman.user_service.dto.TenantResponseDTO;
import com.eman.user_service.model.Tenant;
import com.eman.user_service.model.TenantStatus;
import com.eman.user_service.model.SubscriptionPlan;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

/**
 * Mapper for Tenant entity to/from DTO
 */
@Mapper(componentModel = "spring",
        imports = {
                TenantStatus.class,
                SubscriptionPlan.class
        })
public interface TenantMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", expression = "java(TenantStatus.ACTIVE)")
    @Mapping(target = "subscriptionPlan", expression = "java(SubscriptionPlan.FREE)")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Tenant toEntity(TenantRequestDTO dto);

//    @Mapping(target = "status", expression = "java(tenant.getStatus().name())")
//    @Mapping(target = "subscriptionPlan", expression = "java(tenant.getSubscriptionPlan().name())")
//    @Mapping(target = "status", constant = "ACTIVE")
//    @Mapping(target = "subscriptionPlan", constant = "FREE")
    TenantResponseDTO toDto(Tenant tenant);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "subscriptionPlan", ignore = true)
    void updateEntity(@MappingTarget Tenant tenant, TenantRequestDTO dto);
}