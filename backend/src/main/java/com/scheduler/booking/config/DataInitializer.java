package com.scheduler.booking.config;

import com.scheduler.booking.model.Tenant;
import com.scheduler.booking.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Data initializer for creating sample data.
 *
 * Note: User authentication is now handled by Clerk.
 * To create users (Admin, Business, Customer):
 * 1. Use Clerk Dashboard -> Users -> Create User
 * 2. Set the user's role in public_metadata: { "role": "ADMIN" | "BUSINESS" |
 * "CUSTOMER" }
 * 3. Then create the corresponding record in your database with clerkUserId
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final TenantRepository tenantRepository;
    private final com.scheduler.booking.repository.BusinessUserRepository businessUserRepository;

    @Override
    public void run(String... args) {
        log.info("===========================================");
        log.info("Authentication is now handled by Clerk!");
        log.info("To create admin users:");
        log.info("1. Go to Clerk Dashboard -> Users -> Create User");
        log.info("2. Create user with email and password");
        log.info("3. Set public_metadata: { \"role\": \"ADMIN\" }");
        log.info("4. Create Admin record in database with clerkUserId");
        log.info("===========================================");

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
            tenant = tenantRepository.save(tenant);
            log.info("Created demo tenant: demo-yoga");

            // Create demo business user (unlinked initially)
            com.scheduler.booking.model.BusinessUser businessUser = new com.scheduler.booking.model.BusinessUser();
            businessUser.setTenantId(tenant.getId());
            businessUser.setEmail("demo@yogastudio.com");
            businessUser.setFirstName("Demo");
            businessUser.setLastName("Owner");
            businessUser.setRole("OWNER");
            businessUser.setActive(true);
            // clerkUserId is null initially - will be linked on first login via JIT

            businessUserRepository.save(businessUser);
            log.info("Created demo business user: demo@yogastudio.com");
        }

        // Create test business user for test_business Clerk user
        // This user is directly linked to the Clerk user ID
        if (businessUserRepository.findByClerkUserId("user_36O5AICSwOtpNTGrRyG8XgYV8tz").isEmpty()) {
            Tenant tenant = tenantRepository.findBySlug("demo-yoga").orElse(null);
            if (tenant != null) {
                com.scheduler.booking.model.BusinessUser testUser = new com.scheduler.booking.model.BusinessUser();
                testUser.setTenantId(tenant.getId());
                testUser.setClerkUserId("user_36O5AICSwOtpNTGrRyG8XgYV8tz");
                testUser.setEmail("test_business@example.com");
                testUser.setFirstName("Test");
                testUser.setLastName("Business");
                testUser.setRole("OWNER");
                testUser.setActive(true);

                businessUserRepository.save(testUser);
                log.info("Created test business user with Clerk ID: user_36O5AICSwOtpNTGrRyG8XgYV8tz");
            }
        }
    }
}
