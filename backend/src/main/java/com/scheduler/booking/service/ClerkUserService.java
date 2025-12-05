package com.scheduler.booking.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * Service for interacting with Clerk Backend API to manage users.
 */
@Service
@Slf4j
public class ClerkUserService {

    @Value("${clerk.secret-key}")
    private String clerkSecretKey;

    private static final String CLERK_API_BASE = "https://api.clerk.com/v1";
    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Create a new user in Clerk with email and password
     *
     * @param email     User email
     * @param password  User password
     * @param firstName User first name
     * @param lastName  User last name
     * @param role      User role (ADMIN, BUSINESS, CUSTOMER)
     * @param tenantId  Optional tenant ID for business users
     * @return Clerk user ID
     */
    public String createUser(String email, String password, String firstName, String lastName,
            String role, String tenantId) {
        try {
            String url = CLERK_API_BASE + "/users";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(clerkSecretKey);

            Map<String, Object> body = new HashMap<>();
            body.put("email_address", new String[] { email });
            body.put("password", password);
            body.put("first_name", firstName);
            body.put("last_name", lastName);
            body.put("skip_password_checks", true); // For demo purposes
            body.put("skip_password_requirement", false);

            // Add role to public metadata
            Map<String, Object> publicMetadata = new HashMap<>();
            publicMetadata.put("role", role);
            if (tenantId != null) {
                publicMetadata.put("tenant_id", tenantId);
            }
            body.put("public_metadata", publicMetadata);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            log.info("Creating Clerk user with email: {}", email);
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                String clerkUserId = (String) response.getBody().get("id");
                log.info("Successfully created Clerk user with ID: {}", clerkUserId);
                return clerkUserId;
            } else {
                log.error("Failed to create user in Clerk. Status: {}, Body: {}", response.getStatusCode(),
                        response.getBody());
                throw new RuntimeException("Failed to create user in Clerk: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Error creating user in Clerk: {}", e.getMessage(), e);
            throw new RuntimeException("Error creating user in Clerk: " + e.getMessage(), e);
        }
    }

    /**
     * Update user metadata in Clerk
     *
     * @param clerkUserId    Clerk user ID
     * @param publicMetadata Metadata to update
     */
    public void updateUserMetadata(String clerkUserId, Map<String, Object> publicMetadata) {
        try {
            String url = CLERK_API_BASE + "/users/" + clerkUserId + "/metadata";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(clerkSecretKey);

            Map<String, Object> body = new HashMap<>();
            body.put("public_metadata", publicMetadata);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            restTemplate.patchForObject(url, request, Map.class);
            log.info("Updated metadata for Clerk user: {}", clerkUserId);

        } catch (Exception e) {
            log.error("Error updating user metadata in Clerk: {}", e.getMessage(), e);
            throw new RuntimeException("Error updating user metadata in Clerk: " + e.getMessage(), e);
        }
    }

    /**
     * Delete a user from Clerk
     *
     * @param clerkUserId Clerk user ID
     */
    public void deleteUser(String clerkUserId) {
        try {
            String url = CLERK_API_BASE + "/users/" + clerkUserId;

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(clerkSecretKey);

            HttpEntity<Void> request = new HttpEntity<>(headers);
            restTemplate.exchange(url, HttpMethod.DELETE, request, Void.class);

            log.info("Deleted Clerk user: {}", clerkUserId);

        } catch (Exception e) {
            log.error("Error deleting user from Clerk: {}", e.getMessage(), e);
            throw new RuntimeException("Error deleting user from Clerk: " + e.getMessage(), e);
        }
    }

    /**
     * Get user from Clerk by ID
     *
     * @param clerkUserId Clerk user ID
     * @return User data
     */
    public Map<String, Object> getUser(String clerkUserId) {
        try {
            String url = CLERK_API_BASE + "/users/" + clerkUserId;

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(clerkSecretKey);

            HttpEntity<Void> request = new HttpEntity<>(headers);
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, request, Map.class);

            return response.getBody();

        } catch (Exception e) {
            log.error("Error fetching user from Clerk: {}", e.getMessage(), e);
            throw new RuntimeException("Error fetching user from Clerk: " + e.getMessage(), e);
        }
    }
}
