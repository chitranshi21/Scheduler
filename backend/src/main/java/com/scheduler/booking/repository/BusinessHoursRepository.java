package com.scheduler.booking.repository;

import com.scheduler.booking.model.BusinessHours;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.util.List;
import java.util.UUID;

@Repository
public interface BusinessHoursRepository extends JpaRepository<BusinessHours, UUID> {

    List<BusinessHours> findByTenantIdOrderByDayOfWeekAscStartTimeAsc(UUID tenantId);

    List<BusinessHours> findByTenantIdAndDayOfWeek(UUID tenantId, DayOfWeek dayOfWeek);

    void deleteByTenantId(UUID tenantId);
}
