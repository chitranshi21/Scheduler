package com.scheduler.booking.repository;

import com.scheduler.booking.model.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BookingRepository extends JpaRepository<Booking, UUID> {
    List<Booking> findByTenantId(UUID tenantId);
    List<Booking> findByCustomerId(UUID customerId);
    List<Booking> findByTenantIdAndStatus(UUID tenantId, String status);
    Optional<Booking> findByIdAndTenantId(UUID id, UUID tenantId);
    List<Booking> findByStartTimeBetween(LocalDateTime start, LocalDateTime end);

    @Query("SELECT b FROM Booking b WHERE b.tenantId = :tenantId AND b.startTime >= :from ORDER BY b.startTime ASC")
    List<Booking> findUpcomingBookings(UUID tenantId, LocalDateTime from);
}
