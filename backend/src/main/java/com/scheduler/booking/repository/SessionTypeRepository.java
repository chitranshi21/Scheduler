package com.scheduler.booking.repository;

import com.scheduler.booking.model.SessionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SessionTypeRepository extends JpaRepository<SessionType, UUID> {
    List<SessionType> findByTenantId(UUID tenantId);
    List<SessionType> findByTenantIdAndIsActive(UUID tenantId, boolean isActive);
    Optional<SessionType> findByIdAndTenantId(UUID id, UUID tenantId);
}
