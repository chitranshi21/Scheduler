package com.scheduler.booking.repository;

import com.scheduler.booking.model.BlockedSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface BlockedSlotRepository extends JpaRepository<BlockedSlot, UUID> {

    List<BlockedSlot> findByTenantId(UUID tenantId);

    @Query("SELECT b FROM BlockedSlot b WHERE b.tenantId = :tenantId " +
           "AND b.startTime >= :startDate AND b.startTime < :endDate")
    List<BlockedSlot> findByTenantIdAndDateRange(
        @Param("tenantId") UUID tenantId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    @Query("SELECT b FROM BlockedSlot b WHERE b.tenantId = :tenantId " +
           "AND ((b.startTime <= :startTime AND b.endTime > :startTime) " +
           "OR (b.startTime < :endTime AND b.endTime >= :endTime) " +
           "OR (b.startTime >= :startTime AND b.endTime <= :endTime))")
    List<BlockedSlot> findConflictingSlots(
        @Param("tenantId") UUID tenantId,
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime
    );
}
