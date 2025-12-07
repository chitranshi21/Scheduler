package com.scheduler.booking.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "payments")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID bookingId;

    @Column(nullable = false)
    private UUID tenantId;

    @Column(nullable = false)
    private UUID customerId;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private String currency = "USD";

    @Column(nullable = false)
    private String status = "PENDING";

    private String paymentMethod;
    private String stripePaymentIntentId;
    private String stripeChargeId;
    private String stripeCheckoutSessionId;

    @Column(precision = 10, scale = 2)
    private BigDecimal platformFee = BigDecimal.ZERO;

    @Column(precision = 10, scale = 2)
    private BigDecimal businessAmount = BigDecimal.ZERO;

    @Column(precision = 10, scale = 2)
    private BigDecimal refundAmount = BigDecimal.ZERO;

    private LocalDateTime refundedAt;

    @Column(columnDefinition = "TEXT")
    private String failureReason;

    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> metadata;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
