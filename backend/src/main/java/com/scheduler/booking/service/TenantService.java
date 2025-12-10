package com.scheduler.booking.service;

import com.scheduler.booking.dto.TenantRequest;
import com.scheduler.booking.model.BusinessUser;
import com.scheduler.booking.model.Tenant;
import com.scheduler.booking.repository.BusinessUserRepository;
import com.scheduler.booking.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TenantService {

    private final TenantRepository tenantRepository;
    private final BusinessUserRepository businessUserRepository;
    private final ClerkUserService clerkUserService;
    private final BusinessHoursService businessHoursService;

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

        // Create tenant first
        Tenant tenant = new Tenant();
        tenant.setName(request.getName());
        tenant.setSlug(request.getSlug());
        tenant.setEmail(request.getEmail());
        tenant.setPhone(request.getPhone());
        tenant.setLogoUrl(request.getLogoUrl());
        tenant.setDescription(request.getDescription());
        tenant.setBrandColors(request.getBrandColors());
        tenant.setSubscriptionTier(request.getSubscriptionTier() != null ? request.getSubscriptionTier() : "BASIC");
        tenant.setTimezone(request.getTimezone() != null ? request.getTimezone() : "UTC");
        tenant.setStatus("ACTIVE");

        tenant = tenantRepository.save(tenant);
        log.info("Created tenant: {} with ID: {}", tenant.getName(), tenant.getId());

        // Initialize default business hours (9 AM - 5 PM, Monday to Friday)
        businessHoursService.initializeDefaultBusinessHours(tenant.getId());
        log.info("Initialized default business hours for tenant: {}", tenant.getId());

        // Create business owner user in Clerk if details provided
        if (request.getOwnerEmail() != null && request.getOwnerPassword() != null) {
            String firstName = request.getOwnerFirstName() != null ? request.getOwnerFirstName() : "Business";
            String lastName = request.getOwnerLastName() != null ? request.getOwnerLastName() : "Owner";

            // Create user in Clerk
            // Note: If this fails, the RuntimeException will propagate and cause the
            // @Transactional createTenant method to rollback the tenant creation.
            String clerkUserId = clerkUserService.createUser(
                    request.getOwnerEmail(),
                    request.getOwnerPassword(),
                    firstName,
                    lastName,
                    "BUSINESS",
                    tenant.getId().toString());

            log.info("Created Clerk user for business owner: {}", clerkUserId);

            // Create BusinessUser record
            BusinessUser businessUser = new BusinessUser();
            businessUser.setTenantId(tenant.getId());
            businessUser.setEmail(request.getOwnerEmail());
            businessUser.setClerkUserId(clerkUserId);
            businessUser.setFirstName(firstName);
            businessUser.setLastName(lastName);
            businessUser.setRole("OWNER");
            businessUser.setActive(true);

            businessUserRepository.save(businessUser);
            log.info("Created BusinessUser record for tenant: {}", tenant.getId());
        }

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
        if (request.getTimezone() != null) {
            tenant.setTimezone(request.getTimezone());
        }
        return tenantRepository.save(tenant);
    }

    @Transactional
    public void deleteTenant(UUID id) {
        Tenant tenant = getTenantById(id);
        tenant.setStatus("DELETED");
        tenantRepository.save(tenant);
    }

    /**
     * Get business user email for a tenant (for notifications)
     */
    public String getBusinessEmailForTenant(UUID tenantId) {
        return businessUserRepository.findByTenantId(tenantId).stream()
                .filter(BusinessUser::isActive)
                .findFirst()
                .map(BusinessUser::getEmail)
                .orElseThrow(() -> new RuntimeException("No active business user found for tenant"));
    }
}
