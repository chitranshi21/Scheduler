package com.scheduler.booking.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Converts JWT tokens from Clerk into Spring Security Authentication objects.
 * Extracts the user role from the public_metadata claim in the JWT.
 */
@Component
public class ClerkJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Collection<GrantedAuthority> authorities = extractAuthorities(jwt);
        return new JwtAuthenticationToken(jwt, authorities);
    }

    private Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
        List<GrantedAuthority> authorities = new ArrayList<>();

        // Extract role from public_metadata claim
        Map<String, Object> publicMetadata = jwt.getClaim("public_metadata");

        // Debug logging
        System.out.println("=== CLERK JWT DEBUG ===");
        System.out.println("User: " + jwt.getClaim("email"));
        System.out.println("Public Metadata: " + publicMetadata);

        if (publicMetadata != null && publicMetadata.containsKey("role")) {
            String role = (String) publicMetadata.get("role");
            System.out.println("Extracted Role: " + role);
            System.out.println("Granted Authority: ROLE_" + role.toUpperCase());
            // Spring Security requires roles to be prefixed with ROLE_
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()));
        } else {
            System.out.println("No role found in public_metadata - defaulting to CUSTOMER");
            // Default role if none specified
            authorities.add(new SimpleGrantedAuthority("ROLE_CUSTOMER"));
        }

        // Also add the base authenticated authority
        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));

        System.out.println("Final Authorities: " + authorities);
        System.out.println("======================");

        return authorities;
    }

    /**
     * Helper method to extract the Clerk user ID from JWT
     */
    public static String extractClerkUserId(Jwt jwt) {
        return jwt.getSubject(); // 'sub' claim contains the Clerk user ID
    }

    /**
     * Helper method to extract the user email from JWT
     */
    public static String extractEmail(Jwt jwt) {
        // Try different email claims that Clerk might use
        String email = jwt.getClaim("email");
        if (email != null) {
            return email;
        }

        // Try email_addresses array
        Object emailAddresses = jwt.getClaim("email_addresses");
        if (emailAddresses instanceof java.util.List) {
            java.util.List<?> emailList = (java.util.List<?>) emailAddresses;
            if (!emailList.isEmpty() && emailList.get(0) instanceof String) {
                return (String) emailList.get(0);
            }
        }

        // Try primary_email_address
        email = jwt.getClaim("primary_email_address");
        if (email != null) {
            return email;
        }

        return null;
    }

    /**
     * Helper method to extract the user role from JWT
     */
    public static String extractRole(Jwt jwt) {
        Map<String, Object> publicMetadata = jwt.getClaim("public_metadata");
        if (publicMetadata != null && publicMetadata.containsKey("role")) {
            return (String) publicMetadata.get("role");
        }
        return "CUSTOMER"; // default
    }

    /**
     * Helper method to extract tenant ID from JWT (for BUSINESS role)
     */
    public static String extractTenantId(Jwt jwt) {
        Map<String, Object> publicMetadata = jwt.getClaim("public_metadata");
        if (publicMetadata != null && publicMetadata.containsKey("tenant_id")) {
            Object tenantId = publicMetadata.get("tenant_id");
            return tenantId != null ? tenantId.toString() : null;
        }
        return null;
    }

    /**
     * Helper method to extract the user's first name from JWT
     */
    public static String extractFirstName(Jwt jwt) {
        return jwt.getClaim("given_name");
    }

    /**
     * Helper method to extract the user's last name from JWT
     */
    public static String extractLastName(Jwt jwt) {
        return jwt.getClaim("family_name");
    }
}
