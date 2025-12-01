package com.scheduler.booking.repository;

import com.scheduler.booking.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    Optional<Payment> findByBookingId(UUID bookingId);
    List<Payment> findByTenantId(UUID tenantId);
    List<Payment> findByCustomerId(UUID customerId);
}
