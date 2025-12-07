package com.scheduler.booking.service;

import com.scheduler.booking.dto.BookingRequest;
import com.scheduler.booking.model.BlockedSlot;
import com.scheduler.booking.model.Booking;
import com.scheduler.booking.model.Customer;
import com.scheduler.booking.model.SessionType;
import com.scheduler.booking.repository.BlockedSlotRepository;
import com.scheduler.booking.repository.BookingRepository;
import com.scheduler.booking.repository.CustomerRepository;
import com.scheduler.booking.repository.SessionTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final SessionTypeRepository sessionTypeRepository;
    private final CustomerRepository customerRepository;
    private final BlockedSlotRepository blockedSlotRepository;
    private final TenantService tenantService;
    private final EmailService emailService;

    public List<Booking> getBookingsByTenant(UUID tenantId) {
        return bookingRepository.findByTenantId(tenantId);
    }

    public List<Booking> getUpcomingBookings(UUID tenantId) {
        return bookingRepository.findUpcomingBookingsWithDetails(tenantId, LocalDateTime.now());
    }

    public List<Booking> getBookingsByCustomer(UUID customerId) {
        return bookingRepository.findByCustomerId(customerId);
    }

    public Booking getBookingById(UUID id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Booking not found"));
    }

    @Transactional
    public Booking createBooking(UUID tenantId, BookingRequest request, UUID customerId) {
        SessionType sessionType = sessionTypeRepository.findByIdAndTenantId(
                request.getSessionTypeId(), tenantId)
                .orElseThrow(() -> new RuntimeException("Session type not found"));

        // Convert epoch timestamp to LocalDateTime
        java.time.Instant startInstant = java.time.Instant.ofEpochMilli(request.getStartTime());
        LocalDateTime startTime = LocalDateTime.ofInstant(startInstant, java.time.ZoneId.systemDefault());

        // Calculate end time
        LocalDateTime endTime = startTime.plusMinutes(sessionType.getDurationMinutes());

        // Check if the time slot is blocked
        List<BlockedSlot> conflictingBlocks = blockedSlotRepository.findConflictingSlots(
                tenantId, startTime, endTime);

        if (!conflictingBlocks.isEmpty()) {
            throw new RuntimeException("This time slot is not available. Please choose another time.");
        }

        // If customer ID not provided, create or find customer by email
        if (customerId == null && request.getEmail() != null) {
            Customer customer = customerRepository.findByEmail(request.getEmail())
                    .orElseGet(() -> {
                        Customer newCustomer = new Customer();
                        newCustomer.setEmail(request.getEmail());
                        newCustomer.setFirstName(request.getFirstName());
                        newCustomer.setLastName(request.getLastName());
                        newCustomer.setPhone(request.getPhone());
                        return customerRepository.save(newCustomer);
                    });
            customerId = customer.getId();
        }

        if (customerId == null) {
            throw new RuntimeException("Customer information is required");
        }

        Booking booking = new Booking();
        booking.setTenantId(tenantId);
        booking.setCustomerId(customerId);
        booking.setSessionTypeId(sessionType.getId());
        booking.setStartTime(startTime);
        booking.setEndTime(endTime);
        booking.setParticipants(request.getParticipants());
        booking.setNotes(request.getNotes());
        booking.setCustomerTimezone(request.getCustomerTimezone());
        booking.setStatus("PENDING_PAYMENT"); // Will be updated to CONFIRMED after successful payment

        Booking savedBooking = bookingRepository.save(booking);

        // Reload booking with full relationships for email
        Booking fullBooking = bookingRepository.findByIdWithDetails(savedBooking.getId())
                .orElseThrow(() -> new RuntimeException("Booking not found after save"));

        // Manually set relationships to ensure they're loaded for email templates
        // This is necessary because JPA lazy/eager loading can be unreliable
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Customer not found"));
        fullBooking.setCustomer(customer);
        fullBooking.setSessionType(sessionType);

        // Send confirmation emails asynchronously
        try {
            var tenant = tenantService.getTenantById(tenantId);
            var businessUser = tenantService.getBusinessEmailForTenant(tenantId);

            emailService.sendCustomerBookingConfirmation(fullBooking, tenant);
            emailService.sendBusinessBookingNotification(fullBooking, tenant, businessUser);
        } catch (Exception e) {
            // Log error but don't fail the booking
            System.err.println("Failed to send booking confirmation emails: " + e.getMessage());
        }

        return savedBooking;
    }

    @Transactional
    public void cancelBooking(UUID id, String reason) {
        Booking booking = getBookingById(id);
        booking.setStatus("CANCELLED");
        booking.setCancellationReason(reason);
        booking.setCancelledAt(LocalDateTime.now());
        bookingRepository.save(booking);
    }
}
