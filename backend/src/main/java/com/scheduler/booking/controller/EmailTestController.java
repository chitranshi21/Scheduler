package com.scheduler.booking.controller;

import com.scheduler.booking.model.*;
import com.scheduler.booking.repository.*;
import com.scheduler.booking.service.CalendarService;
import com.scheduler.booking.service.EmailService;
import com.scheduler.booking.service.TenantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Test controller for email functionality
 * This is a temporary controller for testing email sending
 */
@RestController
@RequestMapping("/api/auth/test-email")
@RequiredArgsConstructor
@Slf4j
public class EmailTestController {

    private final EmailService emailService;
    private final TenantService tenantService;
    private final CustomerRepository customerRepository;
    private final SessionTypeRepository sessionTypeRepository;
    private final TenantRepository tenantRepository;
    private final BookingRepository bookingRepository;
    private final CalendarService calendarService;

    @PostMapping("/send")
    public String sendTestEmail(@RequestParam String toEmail) {
        try {
            // Get demo tenant
            Tenant tenant = tenantRepository.findBySlug("demo-yoga")
                    .orElseThrow(() -> new RuntimeException("Demo tenant not found"));

            // Create or get customer
            Customer customer = customerRepository.findByEmail(toEmail)
                    .orElseGet(() -> {
                        Customer newCustomer = new Customer();
                        newCustomer.setEmail(toEmail);
                        newCustomer.setFirstName("Test");
                        newCustomer.setLastName("Customer");
                        newCustomer.setPhone("+1234567890");
                        return customerRepository.save(newCustomer);
                    });

            // Create a session type if it doesn't exist
            SessionType sessionType = sessionTypeRepository.findByTenantId(tenant.getId())
                    .stream()
                    .findFirst()
                    .orElseGet(() -> {
                        SessionType newType = new SessionType();
                        newType.setTenantId(tenant.getId());
                        newType.setName("Test Yoga Session");
                        newType.setDescription("A relaxing test yoga session");
                        newType.setDurationMinutes(60);
                        newType.setPrice(new BigDecimal("50.00"));
                        newType.setColor("#667eea");
                        newType.setActive(true);
                        return sessionTypeRepository.save(newType);
                    });

            // Create a test booking
            Booking booking = new Booking();
            booking.setTenantId(tenant.getId());
            booking.setCustomerId(customer.getId());
            booking.setSessionTypeId(sessionType.getId());
            booking.setStartTime(LocalDateTime.now().plusDays(1)); // Tomorrow
            booking.setEndTime(LocalDateTime.now().plusDays(1).plusMinutes(sessionType.getDurationMinutes()));
            booking.setParticipants(1);
            booking.setNotes("This is a test booking to verify email functionality");
            booking.setCustomerTimezone("America/New_York");
            booking.setStatus("CONFIRMED");

            // Save booking
            booking = bookingRepository.save(booking);

            // Manually set the relationships for email
            booking.setCustomer(customer);
            booking.setSessionType(sessionType);

            // Get business email
            String businessEmail = tenantService.getBusinessEmailForTenant(tenant.getId());

            // Send emails
            log.info("Sending test emails to customer: {} and business: {}", toEmail, businessEmail);
            emailService.sendCustomerBookingConfirmation(booking, tenant);
            emailService.sendBusinessBookingNotification(booking, tenant, businessEmail);

            return "Test emails triggered successfully! Check logs for details. Customer: " + toEmail + ", Business: " + businessEmail;

        } catch (Exception e) {
            log.error("Error sending test email", e);
            return "Error: " + e.getMessage();
        }
    }

    @GetMapping("/info")
    public String getInfo() {
        return "Email Test Controller - Use POST /api/auth/test-email/send?toEmail=your-email@example.com to test email sending";
    }
}
