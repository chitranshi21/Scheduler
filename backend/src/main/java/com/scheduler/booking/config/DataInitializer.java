package com.scheduler.booking.config;

import com.scheduler.booking.model.Admin;
import com.scheduler.booking.model.Tenant;
import com.scheduler.booking.repository.AdminRepository;
import com.scheduler.booking.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final AdminRepository adminRepository;
    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        // Create default admin user
        if (!adminRepository.existsByEmail("admin")) {
            Admin admin = new Admin();
            admin.setEmail("admin");
            admin.setPasswordHash(passwordEncoder.encode("admin"));
            admin.setFirstName("System");
            admin.setLastName("Administrator");
            admin.setRole("ADMIN");
            admin.setActive(true);
            adminRepository.save(admin);
            log.info("Created default admin user - username: admin, password: admin");
        }

        // Create a sample tenant for testing
        if (!tenantRepository.existsBySlug("demo-yoga")) {
            Tenant tenant = new Tenant();
            tenant.setName("Demo Yoga Studio");
            tenant.setSlug("demo-yoga");
            tenant.setEmail("demo@yogastudio.com");
            tenant.setPhone("555-0123");
            tenant.setDescription("A demo yoga studio for testing the booking platform");
            tenant.setStatus("ACTIVE");
            tenant.setSubscriptionTier("BASIC");
            tenantRepository.save(tenant);
            log.info("Created demo tenant: demo-yoga");
        }
    }
}
