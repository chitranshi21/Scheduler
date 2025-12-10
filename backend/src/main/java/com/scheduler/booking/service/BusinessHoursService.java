package com.scheduler.booking.service;

import com.scheduler.booking.dto.BusinessHoursRequest;
import com.scheduler.booking.dto.BusinessHoursResponse;
import com.scheduler.booking.model.BusinessHours;
import com.scheduler.booking.repository.BusinessHoursRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BusinessHoursService {

    private final BusinessHoursRepository businessHoursRepository;

    public List<BusinessHoursResponse> getBusinessHours(UUID tenantId) {
        List<BusinessHours> hours = businessHoursRepository.findByTenantIdOrderByDayOfWeekAscStartTimeAsc(tenantId);
        return hours.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void initializeDefaultBusinessHours(UUID tenantId) {
        // Create default business hours: Monday to Friday, 9 AM - 5 PM
        List<BusinessHours> defaultHours = new ArrayList<>();

        DayOfWeek[] weekdays = {
            DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY, DayOfWeek.FRIDAY
        };

        LocalTime startTime = LocalTime.of(9, 0);  // 9 AM
        LocalTime endTime = LocalTime.of(17, 0);   // 5 PM

        for (DayOfWeek day : weekdays) {
            BusinessHours hours = new BusinessHours();
            hours.setTenantId(tenantId);
            hours.setDayOfWeek(day);
            hours.setStartTime(startTime);
            hours.setEndTime(endTime);
            hours.setEnabled(true);
            defaultHours.add(hours);
        }

        businessHoursRepository.saveAll(defaultHours);
    }

    @Transactional
    public BusinessHoursResponse createBusinessHours(UUID tenantId, BusinessHoursRequest request) {
        BusinessHours hours = new BusinessHours();
        hours.setTenantId(tenantId);
        hours.setDayOfWeek(request.getDayOfWeek());
        hours.setStartTime(LocalTime.parse(request.getStartTime()));
        hours.setEndTime(LocalTime.parse(request.getEndTime()));
        hours.setEnabled(request.isEnabled());

        BusinessHours saved = businessHoursRepository.save(hours);
        return mapToResponse(saved);
    }

    @Transactional
    public BusinessHoursResponse updateBusinessHours(UUID id, UUID tenantId, BusinessHoursRequest request) {
        BusinessHours hours = businessHoursRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Business hours not found"));

        if (!hours.getTenantId().equals(tenantId)) {
            throw new RuntimeException("Unauthorized access to business hours");
        }

        hours.setDayOfWeek(request.getDayOfWeek());
        hours.setStartTime(LocalTime.parse(request.getStartTime()));
        hours.setEndTime(LocalTime.parse(request.getEndTime()));
        hours.setEnabled(request.isEnabled());

        BusinessHours saved = businessHoursRepository.save(hours);
        return mapToResponse(saved);
    }

    @Transactional
    public void deleteBusinessHours(UUID id, UUID tenantId) {
        BusinessHours hours = businessHoursRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Business hours not found"));

        if (!hours.getTenantId().equals(tenantId)) {
            throw new RuntimeException("Unauthorized access to business hours");
        }

        businessHoursRepository.delete(hours);
    }

    @Transactional
    public List<BusinessHoursResponse> updateAllBusinessHours(UUID tenantId, List<BusinessHoursRequest> requests) {
        // Delete existing hours for this tenant
        businessHoursRepository.deleteByTenantId(tenantId);

        // Create new hours
        List<BusinessHours> hoursList = requests.stream()
                .map(request -> {
                    BusinessHours hours = new BusinessHours();
                    hours.setTenantId(tenantId);
                    hours.setDayOfWeek(request.getDayOfWeek());
                    hours.setStartTime(LocalTime.parse(request.getStartTime()));
                    hours.setEndTime(LocalTime.parse(request.getEndTime()));
                    hours.setEnabled(request.isEnabled());
                    return hours;
                })
                .collect(Collectors.toList());

        List<BusinessHours> saved = businessHoursRepository.saveAll(hoursList);
        return saved.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private BusinessHoursResponse mapToResponse(BusinessHours hours) {
        return new BusinessHoursResponse(
                hours.getId(),
                hours.getDayOfWeek(),
                hours.getStartTime().toString(),
                hours.getEndTime().toString(),
                hours.isEnabled()
        );
    }

    public boolean isWithinBusinessHours(UUID tenantId, DayOfWeek dayOfWeek, LocalTime time) {
        List<BusinessHours> hours = businessHoursRepository.findByTenantIdAndDayOfWeek(tenantId, dayOfWeek);

        for (BusinessHours hour : hours) {
            if (hour.isEnabled() &&
                !time.isBefore(hour.getStartTime()) &&
                time.isBefore(hour.getEndTime())) {
                return true;
            }
        }

        return false;
    }
}
