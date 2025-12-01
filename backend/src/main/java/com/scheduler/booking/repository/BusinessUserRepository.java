package com.scheduler.booking.repository;

import com.scheduler.booking.model.BusinessUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BusinessUserRepository extends JpaRepository<BusinessUser, UUID> {
    Optional<BusinessUser> findByEmail(String email);
    List<BusinessUser> findByTenantId(UUID tenantId);
    Optional<BusinessUser> findByTenantIdAndEmail(UUID tenantId, String email);
}
