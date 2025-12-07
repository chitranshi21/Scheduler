package com.scheduler.booking.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "bookings")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "session_type_id", nullable = false)
    private UUID sessionTypeId;

    // Transient fields for JSON serialization
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "customer_id", insertable = false, updatable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "session_type_id", insertable = false, updatable = false)
    private SessionType sessionType;

    @Column(nullable = false)
    @JsonFormat(shape = JsonFormat.Shape.NUMBER)
    private LocalDateTime startTime;

    @Column(nullable = false)
    @JsonFormat(shape = JsonFormat.Shape.NUMBER)
    private LocalDateTime endTime;

    @Column(nullable = false)
    private String status = "PENDING";

    private Integer participants = 1;

    @Column(columnDefinition = "TEXT")
    private String notes;

    private String customerTimezone;
    private String cancellationReason;

    @JsonFormat(shape = JsonFormat.Shape.NUMBER)
    private LocalDateTime cancelledAt;

    private String cancelledBy;

    @CreationTimestamp
    @JsonFormat(shape = JsonFormat.Shape.NUMBER)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @JsonFormat(shape = JsonFormat.Shape.NUMBER)
    private LocalDateTime updatedAt;

    public String getConfirmationNumber() {
        return "BK-" + id.toString().substring(0, 8).toUpperCase();
    }
}
