package com.scheduler.booking.controller;

import com.scheduler.booking.security.ClerkJwtAuthenticationConverter;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Auth controller for public endpoints.
 * Authentication is handled by Clerk on the client side.
 * This controller provides endpoints to retrieve current user information from JWT.
 */
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"})
public class AuthController {

    @GetMapping("/test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("API is working!");
    }

    /**
     * Get current authenticated user information from JWT token
     */
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }

        Map<String, Object> userInfo = new HashMap<>();

        // Extract JWT from authentication
        if (authentication.getPrincipal() instanceof Jwt jwt) {
            userInfo.put("clerkUserId", ClerkJwtAuthenticationConverter.extractClerkUserId(jwt));
            userInfo.put("email", ClerkJwtAuthenticationConverter.extractEmail(jwt));
            userInfo.put("firstName", ClerkJwtAuthenticationConverter.extractFirstName(jwt));
            userInfo.put("lastName", ClerkJwtAuthenticationConverter.extractLastName(jwt));
            userInfo.put("role", ClerkJwtAuthenticationConverter.extractRole(jwt));
            userInfo.put("tenantId", ClerkJwtAuthenticationConverter.extractTenantId(jwt));
            userInfo.put("authorities", authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toList()));
        }

        return ResponseEntity.ok(userInfo);
    }
}
