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
     */
    public void handlePaymentSuccess(Event event) {
        try {
            Session session = (Session) event.getDataObjectDeserializer().getObject().orElse(null);
            if (session == null) {
                log.error("Could not deserialize session from event");
                return;
            }

            String bookingId = session.getMetadata().get("booking_id");
            String platformFeeStr = session.getMetadata().get("platform_fee");
            String businessAmountStr = session.getMetadata().get("business_amount");

            // Find or create payment record
            Payment payment = paymentRepository.findByBookingId(java.util.UUID.fromString(bookingId))
                    .orElse(null);

            if (payment != null) {
                payment.setStatus("COMPLETED");
                payment.setStripeCheckoutSessionId(session.getId());
                payment.setPaymentMethod("card");
                payment.setPlatformFee(new BigDecimal(platformFeeStr));
                payment.setBusinessAmount(new BigDecimal(businessAmountStr));

                paymentRepository.save(payment);
                log.info("Payment marked as COMPLETED for booking: {}", bookingId);

                // Update booking status to CONFIRMED
                Booking booking = bookingRepository.findById(java.util.UUID.fromString(bookingId))
                        .orElse(null);
                if (booking != null && "PENDING_PAYMENT".equals(booking.getStatus())) {
                    booking.setStatus("CONFIRMED");
                    bookingRepository.save(booking);
                    log.info("Booking {} status updated to CONFIRMED", bookingId);
                }
            } else {
                log.warn("Payment record not found for booking: {}", bookingId);
            }

        } catch (Exception e) {
            log.error("Error handling payment success", e);
        }
    }

    /**
     * Handle failed payment from webhook
     */
    public void handlePaymentFailed(Event event) {
        try {
            Session session = (Session) event.getDataObjectDeserializer().getObject().orElse(null);
            if (session == null) {
                log.error("Could not deserialize session from event");
                return;
            }

            String bookingId = session.getMetadata().get("booking_id");

            // Find payment record and mark as failed
            Payment payment = paymentRepository.findByBookingId(java.util.UUID.fromString(bookingId))
                    .orElse(null);

            if (payment != null) {
                payment.setStatus("FAILED");
                payment.setFailureReason("Payment failed during checkout");
                paymentRepository.save(payment);
                log.info("Payment marked as FAILED for booking: {}", bookingId);

                // Update booking status to PAYMENT_FAILED
                Booking booking = bookingRepository.findById(java.util.UUID.fromString(bookingId))
                        .orElse(null);
                if (booking != null && "PENDING_PAYMENT".equals(booking.getStatus())) {
                    booking.setStatus("PAYMENT_FAILED");
                    bookingRepository.save(booking);
                    log.info("Booking {} status updated to PAYMENT_FAILED", bookingId);
                }
            }

        } catch (Exception e) {
            log.error("Error handling payment failure", e);
        }
    }
}
