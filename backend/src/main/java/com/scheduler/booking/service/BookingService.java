package com.scheduler.booking.service;

import com.scheduler.booking.config.StripeConfig;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingService {

    private final BookingRepository bookingRepository;
    private final SessionTypeRepository sessionTypeRepository;
    private final CustomerRepository customerRepository;
    private final BlockedSlotRepository blockedSlotRepository;
    private final TenantService tenantService;
    private final EmailService emailService;
    private final StripeConfig stripeConfig;

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
        
        // Determine booking status based on Stripe configuration and session price
        boolean isStripeEnabled = stripeConfig.isEnabled();
        boolean isFreeSession = sessionType.getPrice() == null || 
                               sessionType.getPrice().compareTo(BigDecimal.ZERO) == 0;
        
        if (!isStripeEnabled || isFreeSession) {
            // If Stripe is disabled or session is free, confirm immediately
            booking.setStatus("CONFIRMED");
        } else {
            // If Stripe is enabled and session has a price, wait for payment
            booking.setStatus("PENDING_PAYMENT");
        }

        Booking savedBooking = bookingRepository.save(booking);
        
        log.info("Booking {} created with status: {} (Stripe enabled: {}, Free session: {})", 
                savedBooking.getId(), savedBooking.getStatus(), isStripeEnabled, isFreeSession);

        // If booking is confirmed (Stripe disabled or free session), send emails immediately
        if ("CONFIRMED".equals(savedBooking.getStatus())) {
            log.info("Sending confirmation emails immediately for booking {} (Stripe disabled or free session)", 
                    savedBooking.getId());
            try {
                // Load full booking with relationships for email
                Booking fullBooking = bookingRepository.findByIdWithDetails(savedBooking.getId())
                        .orElse(savedBooking);
                
                // Ensure relationships are loaded
                if (fullBooking.getCustomer() == null) {
                    Customer customer = customerRepository.findById(fullBooking.getCustomerId())
                            .orElseThrow(() -> new RuntimeException("Customer not found"));
                    fullBooking.setCustomer(customer);
                }
                if (fullBooking.getSessionType() == null) {
                    SessionType st = sessionTypeRepository.findById(fullBooking.getSessionTypeId())
                            .orElseThrow(() -> new RuntimeException("Session type not found"));
                    fullBooking.setSessionType(st);
                }
                
                var tenant = tenantService.getTenantById(fullBooking.getTenantId());
                var businessUser = tenantService.getBusinessEmailForTenant(fullBooking.getTenantId());
                
                emailService.sendCustomerBookingConfirmation(fullBooking, tenant);
                emailService.sendBusinessBookingNotification(fullBooking, tenant, businessUser);
                
                log.info("✅ Confirmation emails sent for booking {}", savedBooking.getId());
            } catch (Exception e) {
                // Log error but don't fail the booking creation
                log.error("❌ Failed to send booking confirmation emails for booking {}: {}", 
                        savedBooking.getId(), e.getMessage(), e);
            }
        } else {
            // If payment is required, emails will be sent after successful payment
            // via the Stripe webhook handler (see StripeService.handlePaymentSuccess)
            log.info("Emails will be sent after successful payment for booking {}", savedBooking.getId());
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

    /**
     * Confirm booking after successful payment and send confirmation emails
     * This is called from the Stripe webhook handler
     */
    @Transactional
    public void confirmBookingAfterPayment(UUID bookingId) {
        // Reload booking with full relationships for email
        Booking fullBooking = bookingRepository.findByIdWithDetails(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found: " + bookingId));

        // Update status to CONFIRMED
        fullBooking.setStatus("CONFIRMED");
        bookingRepository.save(fullBooking);

        // Manually set relationships to ensure they're loaded for email templates
        Customer customer = customerRepository.findById(fullBooking.getCustomerId())
                .orElseThrow(() -> new RuntimeException("Customer not found"));
        SessionType sessionType = sessionTypeRepository.findById(fullBooking.getSessionTypeId())
                .orElseThrow(() -> new RuntimeException("Session type not found"));

        fullBooking.setCustomer(customer);
        fullBooking.setSessionType(sessionType);

        // Send confirmation emails asynchronously
        try {
            log.info("Sending confirmation emails for booking {} after successful payment", bookingId);
            var tenant = tenantService.getTenantById(fullBooking.getTenantId());
            var businessUser = tenantService.getBusinessEmailForTenant(fullBooking.getTenantId());

            emailService.sendCustomerBookingConfirmation(fullBooking, tenant);
            emailService.sendBusinessBookingNotification(fullBooking, tenant, businessUser);
            
            log.info("✅ Confirmation emails sent successfully for booking {}", bookingId);
        } catch (Exception e) {
            // Log error but don't fail the booking confirmation
            log.error("❌ Failed to send booking confirmation emails for booking {}: {}", 
                    bookingId, e.getMessage(), e);
        }
    }
}
