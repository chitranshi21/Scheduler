package com.scheduler.booking.controller;

import com.scheduler.booking.config.StripeConfig;
import com.scheduler.booking.model.Booking;
import com.scheduler.booking.model.Payment;
import com.scheduler.booking.model.SessionType;
import com.scheduler.booking.repository.BookingRepository;
import com.scheduler.booking.repository.PaymentRepository;
import com.scheduler.booking.repository.SessionTypeRepository;
import com.scheduler.booking.service.StripeService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/stripe")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"})
public class StripeController {

    private final StripeService stripeService;
    private final StripeConfig stripeConfig;
    private final BookingRepository bookingRepository;
    private final SessionTypeRepository sessionTypeRepository;
    private final PaymentRepository paymentRepository;

    /**
     * Create Stripe Checkout Session for a booking
     */
    @PostMapping("/create-checkout-session")
    public ResponseEntity<Map<String, String>> createCheckoutSession(@RequestBody Map<String, String> request) {
        try {
            String bookingId = request.get("bookingId");

            Booking booking = bookingRepository.findById(UUID.fromString(bookingId))
                    .orElseThrow(() -> new RuntimeException("Booking not found"));

            SessionType sessionType = sessionTypeRepository.findById(booking.getSessionTypeId())
                    .orElseThrow(() -> new RuntimeException("Session type not found"));

            // Create Stripe Checkout Session
            Session session = stripeService.createCheckoutSession(
                    booking,
                    sessionType,
                    booking.getCustomer().getEmail()
            );

            // Create payment record
            Map<String, BigDecimal> fees = stripeService.calculateFees(sessionType.getPrice());

            Payment payment = new Payment();
            payment.setBookingId(booking.getId());
            payment.setTenantId(booking.getTenantId());
            payment.setCustomerId(booking.getCustomerId());
            payment.setAmount(fees.get("totalAmount"));
            payment.setCurrency(sessionType.getCurrency());
            payment.setStatus("PENDING");
            payment.setStripeCheckoutSessionId(session.getId());
            payment.setPlatformFee(fees.get("platformFee"));
            payment.setBusinessAmount(fees.get("businessAmount"));

            paymentRepository.save(payment);

            // Return checkout URL
            Map<String, String> response = new HashMap<>();
            response.put("checkoutUrl", session.getUrl());
            response.put("sessionId", session.getId());

            return ResponseEntity.ok(response);

        } catch (StripeException e) {
            log.error("Stripe error creating checkout session", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to create checkout session: " + e.getMessage()));
        } catch (Exception e) {
            log.error("Error creating checkout session", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get Stripe publishable key for frontend
     */
    @GetMapping("/config")
    public ResponseEntity<Map<String, String>> getConfig() {
        Map<String, String> config = new HashMap<>();
        config.put("publishableKey", stripeConfig.getPublishableKey());
        return ResponseEntity.ok(config);
    }

    /**
     * Webhook endpoint for Stripe events
     */
    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(@RequestBody String payload,
                                               @RequestHeader("Stripe-Signature") String sigHeader) {
        try {
            Event event = Webhook.constructEvent(
                    payload,
                    sigHeader,
                    stripeConfig.getWebhookSecret()
            );

            log.info("Received Stripe webhook event: {}", event.getType());

            // Handle different event types
            switch (event.getType()) {
                case "checkout.session.completed":
                    // Payment succeeded - confirm booking and send emails
                    stripeService.handlePaymentSuccess(event);
                    break;

                case "payment_intent.payment_failed":
                    // Payment was attempted but failed (declined card, etc)
                    stripeService.handlePaymentFailed(event);
                    break;

                case "checkout.session.expired":
                    // User abandoned checkout or session expired (24 hours)
                    stripeService.handlePaymentCancelled(event);
                    break;

                case "payment_intent.canceled":
                    // Payment was cancelled
                    stripeService.handlePaymentCancelled(event);
                    break;

                default:
                    log.info("Unhandled event type: {}", event.getType());
            }

            return ResponseEntity.ok("Webhook handled");

        } catch (SignatureVerificationException e) {
            log.error("Invalid webhook signature", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid signature");
        } catch (Exception e) {
            log.error("Error handling webhook", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Webhook error");
        }
    }

    /**
     * Get payment status for a booking
     */
    @GetMapping("/payment-status/{bookingId}")
    public ResponseEntity<Payment> getPaymentStatus(@PathVariable UUID bookingId) {
        Payment payment = paymentRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));
        return ResponseEntity.ok(payment);
    }
}
