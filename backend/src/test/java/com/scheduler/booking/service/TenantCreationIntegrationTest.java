package com.scheduler.booking.service;

import com.scheduler.booking.dto.TenantRequest;
import com.scheduler.booking.model.Tenant;
import com.scheduler.booking.repository.TenantRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
public class TenantCreationIntegrationTest {

    @Autowired
    private TenantService tenantService;

    @Autowired
    private TenantRepository tenantRepository;

    @MockBean
    private ClerkUserService clerkUserService;

    @Test
    public void testTenantCreationRollbackOnClerkFailure() {
        // Arrange
        TenantRequest request = new TenantRequest();
        request.setName("Rollback Test Tenant");
        request.setSlug("rollback-test");
        request.setEmail("test@example.com");
        request.setOwnerEmail("owner@example.com");
        request.setOwnerPassword("password");
        request.setOwnerFirstName("Test");
        request.setOwnerLastName("Owner");

        // Mock Clerk service to throw exception
        when(clerkUserService.createUser(anyString(), anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenThrow(new RuntimeException("Clerk API failed"));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            tenantService.createTenant(request);
        });

        // Verify tenant was NOT created (rollback successful)
        assertFalse(tenantRepository.existsBySlug("rollback-test"), "Tenant should not exist after rollback");
    }

    @Test
    public void testTenantCreationSuccess() {
        // Arrange
        TenantRequest request = new TenantRequest();
        request.setName("Success Test Tenant");
        request.setSlug("success-test");
        request.setEmail("success@example.com");
        request.setOwnerEmail("success-owner@example.com");
        request.setOwnerPassword("password");

        // Mock Clerk service to return a dummy ID
        when(clerkUserService.createUser(anyString(), anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn("user_12345");

        // Act
        Tenant tenant = tenantService.createTenant(request);

        // Assert
        assertNotNull(tenant);
        assertTrue(tenantRepository.existsBySlug("success-test"));
    }
}
