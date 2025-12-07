package com.scheduler.booking.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "session_types")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SessionType {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID tenantId;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private Integer durationMinutes;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private String currency = "USD";

    private Integer capacity = 1;
    private String category;
    private String color;
    private boolean isActive = true;

    @Column(columnDefinition = "TEXT")
    private String cancellationPolicy;

    @Column(length = 500)
    private String meetingLink;  // Zoom/Meet/Teams link for virtual sessions

    @Column(length = 50)
    private String meetingPassword;  // Optional meeting password

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
