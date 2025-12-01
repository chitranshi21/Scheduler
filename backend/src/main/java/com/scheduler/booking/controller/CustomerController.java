package com.scheduler.booking.controller;

import com.scheduler.booking.dto.BookingRequest;
import com.scheduler.booking.model.Booking;
import com.scheduler.booking.model.SessionType;
import com.scheduler.booking.model.Tenant;
import com.scheduler.booking.repository.TenantRepository;
import com.scheduler.booking.service.BookingService;
import com.scheduler.booking.service.SessionTypeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/customer")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"})
public class CustomerController {

    private final SessionTypeService sessionTypeService;
    private final BookingService bookingService;
    private final TenantRepository tenantRepository;

    @GetMapping("/tenants/{slug}")
    public ResponseEntity<Tenant> getTenantBySlug(@PathVariable String slug) {
        Tenant tenant = tenantRepository.findBySlug(slug)
                .orElseThrow(() -> new RuntimeException("Tenant not found"));
        return ResponseEntity.ok(tenant);
    }

    @GetMapping("/tenants/{tenantId}/sessions")
    public ResponseEntity<List<SessionType>> getAvailableSessions(@PathVariable UUID tenantId) {
        return ResponseEntity.ok(sessionTypeService.getActiveSessionTypesByTenant(tenantId));
    }

    @PostMapping("/tenants/{tenantId}/bookings")
    public ResponseEntity<Booking> createBooking(
            @PathVariable UUID tenantId,
            @Valid @RequestBody BookingRequest request) {
        return ResponseEntity.ok(bookingService.createBooking(tenantId, request, null));
    }

    @GetMapping("/bookings/{id}")
    public ResponseEntity<Booking> getBooking(@PathVariable UUID id) {
        return ResponseEntity.ok(bookingService.getBookingById(id));
    }
}
