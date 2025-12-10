package com.scheduler.booking.controller;

import com.scheduler.booking.dto.BlockedSlotRequest;
import com.scheduler.booking.dto.BookingRequest;
import com.scheduler.booking.dto.BusinessHoursRequest;
import com.scheduler.booking.dto.BusinessHoursResponse;
import com.scheduler.booking.dto.SessionTypeRequest;
import com.scheduler.booking.model.BlockedSlot;
import com.scheduler.booking.model.Booking;
import com.scheduler.booking.model.BusinessUser;
import com.scheduler.booking.model.SessionType;
import com.scheduler.booking.model.Tenant;
import com.scheduler.booking.repository.BlockedSlotRepository;
import com.scheduler.booking.repository.BusinessUserRepository;
import com.scheduler.booking.repository.TenantRepository;
import com.scheduler.booking.service.BookingService;
import com.scheduler.booking.service.SessionTypeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/business")
@RequiredArgsConstructor
@PreAuthorize("hasRole('BUSINESS')")
@CrossOrigin(origins = { "http://localhost:3000", "http://localhost:5173" })
public class BusinessController {

    private final SessionTypeService sessionTypeService;
    private final BookingService bookingService;
    private final BusinessUserRepository businessUserRepository;
    private final TenantRepository tenantRepository;
    private final BlockedSlotRepository blockedSlotRepository;
    private final com.scheduler.booking.service.BusinessHoursService businessHoursService;

    private UUID getTenantIdFromAuth(Authentication authentication) {
        String clerkUserId = authentication.getName();

        System.out.println("=== BUSINESS AUTH DEBUG ===");
        System.out.println("Clerk User ID: " + clerkUserId);
        System.out.println("Authorities: " + authentication.getAuthorities());

        // 1. Try to find by Clerk User ID
        return businessUserRepository.findByClerkUserId(clerkUserId)
                .map(user -> {
                    System.out.println("Found BusinessUser by Clerk ID: " + user.getEmail());
                    return user.getTenantId();
                })
                .orElseGet(() -> {
                    System.out.println("BusinessUser not found by Clerk ID, trying JIT linking...");

                    // 2. If not found, try to find by email (JIT Linking)
                    if (authentication instanceof org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken) {
                        var jwt = ((org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken) authentication)
                                .getToken();
                        String email = com.scheduler.booking.security.ClerkJwtAuthenticationConverter.extractEmail(jwt);

                        System.out.println("Email from JWT: " + email);
                        System.out.println("JWT Claims: " + jwt.getClaims());

                        if (email != null) {
                            return businessUserRepository.findByEmail(email)
                                    .map(user -> {
                                        // Link the user
                                        System.out.println("Linking BusinessUser by email: " + email);
                                        user.setClerkUserId(clerkUserId);
                                        businessUserRepository.save(user);
                                        return user.getTenantId();
                                    })
                                    .orElseThrow(
                                            () -> new RuntimeException("Business user not found for email: " + email));
                        }
                    }
                    System.out.println("No email in JWT token for JIT linking");
                    throw new RuntimeException("Business user not found and no email in token for JIT linking. Clerk User ID: " + clerkUserId);
                });
    }

    @GetMapping("/tenant")
    public ResponseEntity<Tenant> getTenantInfo(Authentication authentication) {
        UUID tenantId = getTenantIdFromAuth(authentication);
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Tenant not found"));
        return ResponseEntity.ok(tenant);
    }

    @PutMapping("/tenant/timezone")
    public ResponseEntity<Tenant> updateTenantTimezone(
            Authentication authentication,
            @RequestBody java.util.Map<String, String> request) {
        UUID tenantId = getTenantIdFromAuth(authentication);
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Tenant not found"));

        String timezone = request.get("timezone");
        if (timezone != null && !timezone.isEmpty()) {
            tenant.setTimezone(timezone);
            tenant = tenantRepository.save(tenant);
        }

        return ResponseEntity.ok(tenant);
    }

    @GetMapping("/sessions")
    public ResponseEntity<List<SessionType>> getSessionTypes(Authentication authentication) {
        UUID tenantId = getTenantIdFromAuth(authentication);
        return ResponseEntity.ok(sessionTypeService.getSessionTypesByTenant(tenantId));
    }

    @PostMapping("/sessions")
    public ResponseEntity<SessionType> createSessionType(
            Authentication authentication,
            @Valid @RequestBody SessionTypeRequest request) {
        UUID tenantId = getTenantIdFromAuth(authentication);
        return ResponseEntity.ok(sessionTypeService.createSessionType(tenantId, request));
    }

    @PutMapping("/sessions/{id}")
    public ResponseEntity<SessionType> updateSessionType(
            Authentication authentication,
            @PathVariable UUID id,
            @Valid @RequestBody SessionTypeRequest request) {
        UUID tenantId = getTenantIdFromAuth(authentication);
        return ResponseEntity.ok(sessionTypeService.updateSessionType(id, tenantId, request));
    }

    @DeleteMapping("/sessions/{id}")
    public ResponseEntity<Void> deleteSessionType(
            Authentication authentication,
            @PathVariable UUID id) {
        UUID tenantId = getTenantIdFromAuth(authentication);
        sessionTypeService.deleteSessionType(id, tenantId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/bookings")
    public ResponseEntity<List<Booking>> getBookings(Authentication authentication) {
        UUID tenantId = getTenantIdFromAuth(authentication);
        List<Booking> bookings = bookingService.getUpcomingBookings(tenantId);

        // Debug logging to see what's being returned
        System.out.println("=== BOOKINGS RESPONSE DEBUG ===");
        System.out.println("Total bookings: " + bookings.size());
        for (Booking booking : bookings) {
            System.out.println("Booking ID: " + booking.getId());
            System.out.println("  Start Time: " + booking.getStartTime());
            System.out.println("  End Time: " + booking.getEndTime());
            System.out.println("  Customer: " + (booking.getCustomer() != null ? booking.getCustomer().getFirstName() + " " + booking.getCustomer().getLastName() : "NULL"));
            System.out.println("  Session Type: " + (booking.getSessionType() != null ? booking.getSessionType().getName() : "NULL"));
            System.out.println("  Customer loaded: " + (booking.getCustomer() != null));
            System.out.println("  SessionType loaded: " + (booking.getSessionType() != null));
        }
        System.out.println("================================");

        return ResponseEntity.ok(bookings);
    }

    @PostMapping("/bookings")
    public ResponseEntity<Booking> createBooking(
            Authentication authentication,
            @Valid @RequestBody BookingRequest request) {
        UUID tenantId = getTenantIdFromAuth(authentication);
        return ResponseEntity.ok(bookingService.createBooking(tenantId, request, null));
    }

    @DeleteMapping("/bookings/{id}")
    public ResponseEntity<Void> cancelBooking(@PathVariable UUID id) {
        bookingService.cancelBooking(id, "Cancelled by business");
        return ResponseEntity.noContent().build();
    }

    // Blocked Slots endpoints
    @GetMapping("/blocked-slots")
    public ResponseEntity<List<BlockedSlot>> getBlockedSlots(Authentication authentication) {
        UUID tenantId = getTenantIdFromAuth(authentication);
        List<BlockedSlot> slots = blockedSlotRepository.findByTenantId(tenantId);

        System.out.println("=== BLOCKED SLOTS RESPONSE DEBUG ===");
        System.out.println("Total blocked slots: " + slots.size());
        for (BlockedSlot slot : slots) {
            System.out.println("Slot ID: " + slot.getId());
            System.out.println("  Start Time (LocalDateTime): " + slot.getStartTime());
            System.out.println("  End Time (LocalDateTime): " + slot.getEndTime());
            System.out.println("  Start epoch: " + slot.getStartTime().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli());
            System.out.println("  End epoch: " + slot.getEndTime().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli());
        }
        System.out.println("====================================");

        return ResponseEntity.ok(slots);
    }

    @PostMapping("/blocked-slots")
    public ResponseEntity<BlockedSlot> createBlockedSlot(
            Authentication authentication,
            @Valid @RequestBody BlockedSlotRequest request) {
        UUID tenantId = getTenantIdFromAuth(authentication);
        String clerkUserId = authentication.getName();

        // Convert epoch timestamps to LocalDateTime
        java.time.Instant startInstant = java.time.Instant.ofEpochMilli(request.getStartTime());
        java.time.Instant endInstant = java.time.Instant.ofEpochMilli(request.getEndTime());

        java.time.LocalDateTime startTime = java.time.LocalDateTime.ofInstant(startInstant, java.time.ZoneId.systemDefault());
        java.time.LocalDateTime endTime = java.time.LocalDateTime.ofInstant(endInstant, java.time.ZoneId.systemDefault());

        BlockedSlot blockedSlot = new BlockedSlot();
        blockedSlot.setTenantId(tenantId);
        blockedSlot.setStartTime(startTime);
        blockedSlot.setEndTime(endTime);
        blockedSlot.setReason(request.getReason());
        blockedSlot.setCreatedBy(clerkUserId);

        return ResponseEntity.ok(blockedSlotRepository.save(blockedSlot));
    }

    @DeleteMapping("/blocked-slots/{id}")
    public ResponseEntity<Void> deleteBlockedSlot(
            Authentication authentication,
            @PathVariable UUID id) {
        UUID tenantId = getTenantIdFromAuth(authentication);

        // Verify the blocked slot belongs to this tenant
        BlockedSlot slot = blockedSlotRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Blocked slot not found"));

        if (!slot.getTenantId().equals(tenantId)) {
            throw new RuntimeException("Unauthorized to delete this blocked slot");
        }

        blockedSlotRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // Business Hours endpoints
    @GetMapping("/business-hours")
    public ResponseEntity<List<BusinessHoursResponse>> getBusinessHours(Authentication authentication) {
        UUID tenantId = getTenantIdFromAuth(authentication);
        return ResponseEntity.ok(businessHoursService.getBusinessHours(tenantId));
    }

    @PostMapping("/business-hours")
    public ResponseEntity<BusinessHoursResponse> createBusinessHours(
            Authentication authentication,
            @Valid @RequestBody BusinessHoursRequest request) {
        UUID tenantId = getTenantIdFromAuth(authentication);
        return ResponseEntity.ok(businessHoursService.createBusinessHours(tenantId, request));
    }

    @PutMapping("/business-hours/{id}")
    public ResponseEntity<BusinessHoursResponse> updateBusinessHours(
            Authentication authentication,
            @PathVariable UUID id,
            @Valid @RequestBody BusinessHoursRequest request) {
        UUID tenantId = getTenantIdFromAuth(authentication);
        return ResponseEntity.ok(businessHoursService.updateBusinessHours(id, tenantId, request));
    }

    @DeleteMapping("/business-hours/{id}")
    public ResponseEntity<Void> deleteBusinessHours(
            Authentication authentication,
            @PathVariable UUID id) {
        UUID tenantId = getTenantIdFromAuth(authentication);
        businessHoursService.deleteBusinessHours(id, tenantId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/business-hours/batch")
    public ResponseEntity<List<BusinessHoursResponse>> updateAllBusinessHours(
            Authentication authentication,
            @Valid @RequestBody List<BusinessHoursRequest> requests) {
        UUID tenantId = getTenantIdFromAuth(authentication);
        return ResponseEntity.ok(businessHoursService.updateAllBusinessHours(tenantId, requests));
    }
}
