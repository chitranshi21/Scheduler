package com.scheduler.booking.controller;

import com.scheduler.booking.model.BusinessUser;
import com.scheduler.booking.repository.BusinessUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;
import java.util.Map;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class BusinessControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BusinessUserRepository businessUserRepository;

    @Autowired
    private com.scheduler.booking.repository.TenantRepository tenantRepository;

    @Test
    public void testGetTenantInfoSuccess_JITLinking() throws Exception {
        // Arrange
        com.scheduler.booking.model.Tenant tenant = new com.scheduler.booking.model.Tenant();
        tenant.setName("JIT Test Tenant");
        tenant.setSlug("jit-test-tenant");
        tenant.setEmail("jit@tenant.com");
        tenant.setStatus("ACTIVE");
        tenant.setSubscriptionTier("BASIC");
        final com.scheduler.booking.model.Tenant savedTenant = tenantRepository.save(tenant);

        BusinessUser user = new BusinessUser();
        user.setTenantId(savedTenant.getId());
        user.setEmail("jit@business.com");
        // clerkUserId is NULL
        user.setFirstName("JIT");
        user.setLastName("User");
        user.setRole("OWNER");
        user.setActive(true);
        businessUserRepository.save(user);

        // Act & Assert
        // Use jwt() to simulate a JWT with specific claims and authorities
        mockMvc.perform(get("/api/business/tenant")
                .with(SecurityMockMvcRequestPostProcessors.jwt()
                        .jwt(jwt -> jwt.claim("email", "jit@business.com")
                                .claim("sub", "clerk_user_jit_123")
                                .claim("public_metadata", Map.of("role", "BUSINESS", "tenant_id", savedTenant.getId().toString())))
                        .authorities(
                                new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_BUSINESS"),
                                new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_USER")
                        )))
                .andExpect(status().isOk());

        // Verify that the user was updated with the Clerk ID
        BusinessUser updatedUser = businessUserRepository.findByEmail("jit@business.com").orElseThrow();
        assert updatedUser.getClerkUserId().equals("clerk_user_jit_123");
    }
}
