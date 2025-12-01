package com.scheduler.booking.service;

import com.scheduler.booking.dto.TenantRequest;
import com.scheduler.booking.model.BusinessUser;
import com.scheduler.booking.model.Tenant;
import com.scheduler.booking.repository.BusinessUserRepository;
import com.scheduler.booking.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TenantService {

    private final TenantRepository tenantRepository;
    private final BusinessUserRepository businessUserRepository;
    private final PasswordEncoder passwordEncoder;

    public List<Tenant> getAllTenants() {
        return tenantRepository.findAll();
    }

    public Tenant getTenantById(UUID id) {
        return tenantRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tenant not found"));
    }

    @Transactional
    public Tenant createTenant(TenantRequest request) {
        if (tenantRepository.existsBySlug(request.getSlug())) {
            throw new RuntimeException("Slug already exists");
        }

        Tenant tenant = new Tenant();
        tenant.setName(request.getName());
        tenant.setSlug(request.getSlug());
        tenant.setEmail(request.getEmail());
        tenant.setPhone(request.getPhone());
        tenant.setLogoUrl(request.getLogoUrl());
        tenant.setDescription(request.getDescription());
        tenant.setBrandColors(request.getBrandColors());
        tenant.setSubscriptionTier(request.getSubscriptionTier() != null ?
                request.getSubscriptionTier() : "BASIC");
        tenant.setStatus("ACTIVE");

        tenant = tenantRepository.save(tenant);

        // Create default business owner account
        BusinessUser owner = new BusinessUser();
        owner.setTenantId(tenant.getId());
        owner.setEmail(request.getEmail());
        owner.setPasswordHash(passwordEncoder.encode("changeme123")); // Default password
        owner.setFirstName("Owner");
        owner.setLastName(tenant.getName());
        owner.setRole("OWNER");
        businessUserRepository.save(owner);

        return tenant;
    }

    @Transactional
    public Tenant updateTenant(UUID id, TenantRequest request) {
        Tenant tenant = getTenantById(id);
        tenant.setName(request.getName());
        tenant.setEmail(request.getEmail());
        tenant.setPhone(request.getPhone());
        tenant.setLogoUrl(request.getLogoUrl());
        tenant.setDescription(request.getDescription());
        tenant.setBrandColors(request.getBrandColors());
        if (request.getSubscriptionTier() != null) {
            tenant.setSubscriptionTier(request.getSubscriptionTier());
        }
        return tenantRepository.save(tenant);
    }

    @Transactional
    public void deleteTenant(UUID id) {
        Tenant tenant = getTenantById(id);
        tenant.setStatus("DELETED");
        tenantRepository.save(tenant);
    }
}
