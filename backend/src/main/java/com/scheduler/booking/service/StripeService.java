package com.scheduler.booking.service;

import com.scheduler.booking.config.StripeConfig;
import com.scheduler.booking.model.Booking;
import com.scheduler.booking.model.Payment;
import com.scheduler.booking.model.SessionType;
import com.scheduler.booking.repository.BookingRepository;
import com.scheduler.booking.repository.PaymentRepository;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class StripeService {

    private final StripeConfig stripeConfig;
    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final BookingService bookingService;

    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    /**
     * Calculate platform fee (5%) and business amount
     */
    public Map<String, BigDecimal> calculateFees(BigDecimal sessionPrice) {
        BigDecimal platformFeePercentage = BigDecimal.valueOf(stripeConfig.getPlatformFeePercentage() / 100);
        BigDecimal platformFee = sessionPrice.multiply(platformFeePercentage).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalAmount = sessionPrice.add(platformFee);
        BigDecimal businessAmount = sessionPrice;

        Map<String, BigDecimal> fees = new HashMap<>();
        fees.put("platformFee", platformFee);
        fees.put("businessAmount", businessAmount);
        fees.put("totalAmount", totalAmount);

        return fees;
    }

    /**
     * Create Stripe Checkout Session for booking payment
     */
    public Session createCheckoutSession(Booking booking, SessionType sessionType, String customerEmail)
            throws StripeException {

        if (!stripeConfig.isEnabled()) {
            throw new RuntimeException("Stripe is not enabled");
        }

        // Calculate fees
        Map<String, BigDecimal> fees = calculateFees(sessionType.getPrice());
        BigDecimal totalAmount = fees.get("totalAmount");

        // Convert to cents (Stripe requires amount in smallest currency unit)
        long amountInCents = totalAmount.multiply(BigDecimal.valueOf(100)).longValue();

        // Create Checkout Session
        SessionCreateParams.Builder paramsBuilder = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(frontendUrl + "/booking-success?session_id={CHECKOUT_SESSION_ID}")
                .setCancelUrl(frontendUrl + "/booking-cancelled")
                .setCustomerEmail(customerEmail)
                .addLineItem(
                        SessionCreateParams.LineItem.builder()
                                .setPriceData(
                                        SessionCreateParams.LineItem.PriceData.builder()
                                                .setCurrency(sessionType.getCurrency().toLowerCase())
                                                .setUnitAmount(amountInCents)
                                                .setProductData(
                                                        SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                .setName(sessionType.getName())
                                                                .setDescription(String.format(
                                                                        "%s - %d minutes (includes 5%% platform fee)",
                                                                        sessionType.getDescription() != null ? sessionType.getDescription() : "Session",
                                                                        sessionType.getDurationMinutes()
                                                                ))
                                                                .build()
                                                )
                                                .build()
                                )
                                .setQuantity(1L)
                                .build()
                );

        // Add metadata
        paramsBuilder.putMetadata("booking_id", booking.getId().toString());
        paramsBuilder.putMetadata("tenant_id", booking.getTenantId().toString());
        paramsBuilder.putMetadata("session_type_id", sessionType.getId().toString());
        paramsBuilder.putMetadata("platform_fee", fees.get("platformFee").toString());
        paramsBuilder.putMetadata("business_amount", fees.get("businessAmount").toString());

        Session session = Session.create(paramsBuilder.build());
        log.info("Created Stripe Checkout Session: {} for booking: {}", session.getId(), booking.getId());

        return session;
    }

    /**
     * Handle successful payment from webhook
     * This is the critical method that confirms bookings and sends emails
     */
    public void handlePaymentSuccess(Event event) {
        try {
            Session session = (Session) event.getDataObjectDeserializer().getObject().orElse(null);
            if (session == null) {
                log.error("Could not deserialize session from event");
                return;
            }

            String bookingIdStr = session.getMetadata().get("booking_id");
            if (bookingIdStr == null) {
                log.error("No booking_id found in session metadata");
                return;
            }

            java.util.UUID bookingId = java.util.UUID.fromString(bookingIdStr);
            String platformFeeStr = session.getMetadata().get("platform_fee");
            String businessAmountStr = session.getMetadata().get("business_amount");

            log.info("Processing successful payment for booking: {}", bookingId);

            // Verify booking exists and is in correct state
            Booking booking = bookingRepository.findById(bookingId).orElse(null);
            if (booking == null) {
                log.error("Booking not found: {}", bookingId);
                return;
            }

            if (!"PENDING_PAYMENT".equals(booking.getStatus())) {
                log.warn("Booking {} is not in PENDING_PAYMENT state. Current status: {}. Skipping.",
                         bookingId, booking.getStatus());
                return;
            }

            // Update or create payment record
            Payment payment = paymentRepository.findByBookingId(bookingId).orElse(null);

            if (payment != null) {
                payment.setStatus("COMPLETED");
                payment.setStripeCheckoutSessionId(session.getId());
                payment.setPaymentMethod("card");

                if (platformFeeStr != null) {
                    payment.setPlatformFee(new BigDecimal(platformFeeStr));
                }
                if (businessAmountStr != null) {
                    payment.setBusinessAmount(new BigDecimal(businessAmountStr));
                }

                paymentRepository.save(payment);
                log.info("✅ Payment marked as COMPLETED for booking: {}", bookingId);
            } else {
                log.warn("Payment record not found for booking: {}. Creating one.", bookingId);
                // Create payment record if it doesn't exist (shouldn't happen but handle it)
                payment = new Payment();
                payment.setBookingId(bookingId);
                payment.setTenantId(booking.getTenantId());
                payment.setCustomerId(booking.getCustomerId());
                payment.setStatus("COMPLETED");
                payment.setStripeCheckoutSessionId(session.getId());
                payment.setPaymentMethod("card");

                if (platformFeeStr != null) {
                    payment.setPlatformFee(new BigDecimal(platformFeeStr));
                }
                if (businessAmountStr != null) {
                    payment.setBusinessAmount(new BigDecimal(businessAmountStr));
                    payment.setAmount(new BigDecimal(businessAmountStr).add(new BigDecimal(platformFeeStr != null ? platformFeeStr : "0")));
                }

                paymentRepository.save(payment);
            }

            // Confirm booking and send emails (this is the critical step!)
            bookingService.confirmBookingAfterPayment(bookingId);
            log.info("✅ Booking {} confirmed and emails sent", bookingId);

        } catch (Exception e) {
            log.error("❌ Error handling payment success", e);
            // Log the full stack trace for debugging
            e.printStackTrace();
        }
    }

    /**
     * Handle failed payment from webhook
     * Marks payment and booking as failed - NO booking is created in the calendar
     */
    public void handlePaymentFailed(Event event) {
        try {
            Session session = (Session) event.getDataObjectDeserializer().getObject().orElse(null);
            if (session == null) {
                log.error("Could not deserialize session from event");
                return;
            }

            String bookingIdStr = session.getMetadata().get("booking_id");
            if (bookingIdStr == null) {
                log.error("No booking_id found in session metadata");
                return;
            }

            java.util.UUID bookingId = java.util.UUID.fromString(bookingIdStr);
            log.info("Processing failed payment for booking: {}", bookingId);

            // Verify booking exists
            Booking booking = bookingRepository.findById(bookingId).orElse(null);
            if (booking == null) {
                log.error("Booking not found: {}", bookingId);
                return;
            }

            // Only process if booking is in PENDING_PAYMENT state
            if (!"PENDING_PAYMENT".equals(booking.getStatus())) {
                log.warn("Booking {} is not in PENDING_PAYMENT state. Current status: {}. Skipping.",
                         bookingId, booking.getStatus());
                return;
            }

            // Find payment record and mark as failed
            Payment payment = paymentRepository.findByBookingId(bookingId).orElse(null);

            if (payment != null) {
                payment.setStatus("FAILED");
                payment.setFailureReason("Payment failed or was declined during checkout");
                paymentRepository.save(payment);
                log.info("❌ Payment marked as FAILED for booking: {}", bookingId);
            }

            // Update booking status to PAYMENT_FAILED (not CONFIRMED)
            booking.setStatus("PAYMENT_FAILED");
            bookingRepository.save(booking);
            log.info("❌ Booking {} status updated to PAYMENT_FAILED - slot remains available", bookingId);

            // Note: NO emails are sent for failed payments
            // Note: The time slot is NOT booked and remains available for other customers

        } catch (Exception e) {
            log.error("❌ Error handling payment failure", e);
            e.printStackTrace();
        }
    }

    /**
     * Handle cancelled/expired checkout session
     * User abandoned the payment or session expired
     */
    public void handlePaymentCancelled(Event event) {
        try {
            Session session = (Session) event.getDataObjectDeserializer().getObject().orElse(null);
            if (session == null) {
                log.error("Could not deserialize session from event");
                return;
            }

            String bookingIdStr = session.getMetadata().get("booking_id");
            if (bookingIdStr == null) {
                log.error("No booking_id found in session metadata");
                return;
            }

            java.util.UUID bookingId = java.util.UUID.fromString(bookingIdStr);
            log.info("Processing cancelled/expired payment for booking: {}", bookingId);

            // Verify booking exists
            Booking booking = bookingRepository.findById(bookingId).orElse(null);
            if (booking == null) {
                log.error("Booking not found: {}", bookingId);
                return;
            }

            // Only process if booking is in PENDING_PAYMENT state
            if (!"PENDING_PAYMENT".equals(booking.getStatus())) {
                log.warn("Booking {} is not in PENDING_PAYMENT state. Current status: {}. Skipping.",
                         bookingId, booking.getStatus());
                return;
            }

            // Find payment record and mark as cancelled
            Payment payment = paymentRepository.findByBookingId(bookingId).orElse(null);

            if (payment != null) {
                payment.setStatus("CANCELLED");
                payment.setFailureReason("Checkout session expired or was cancelled by user");
                paymentRepository.save(payment);
                log.info("⚠️ Payment marked as CANCELLED for booking: {}", bookingId);
            }

            // Update booking status to CANCELLED
            booking.setStatus("CANCELLED");
            booking.setCancellationReason("Payment cancelled or expired");
            bookingRepository.save(booking);
            log.info("⚠️ Booking {} status updated to CANCELLED - slot remains available", bookingId);

            // Note: NO emails are sent
            // Note: The time slot is NOT booked and remains available

        } catch (Exception e) {
            log.error("❌ Error handling payment cancellation", e);
            e.printStackTrace();
        }
    }
}
