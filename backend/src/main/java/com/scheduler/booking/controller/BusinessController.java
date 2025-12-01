package com.scheduler.booking.controller;

import com.scheduler.booking.dto.BookingRequest;
import com.scheduler.booking.dto.SessionTypeRequest;
import com.scheduler.booking.model.Booking;
import com.scheduler.booking.model.BusinessUser;
import com.scheduler.booking.model.SessionType;
import com.scheduler.booking.model.Tenant;
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
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"})
public class BusinessController {

    private final SessionTypeService sessionTypeService;
    private final BookingService bookingService;
    private final BusinessUserRepository businessUserRepository;
    private final TenantRepository tenantRepository;

    private UUID getTenantIdFromAuth(Authentication authentication) {
        BusinessUser user = businessUserRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Business user not found"));
        return user.getTenantId();
    }

    @GetMapping("/tenant")
    public ResponseEntity<Tenant> getTenantInfo(Authentication authentication) {
        UUID tenantId = getTenantIdFromAuth(authentication);
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Tenant not found"));
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
        return ResponseEntity.ok(bookingService.getUpcomingBookings(tenantId));
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
}
