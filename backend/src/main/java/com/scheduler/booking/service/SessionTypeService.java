package com.scheduler.booking.service;

import com.scheduler.booking.dto.SessionTypeRequest;
import com.scheduler.booking.model.SessionType;
import com.scheduler.booking.repository.SessionTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SessionTypeService {

    private final SessionTypeRepository sessionTypeRepository;

    public List<SessionType> getSessionTypesByTenant(UUID tenantId) {
        return sessionTypeRepository.findByTenantId(tenantId);
    }

    public List<SessionType> getActiveSessionTypesByTenant(UUID tenantId) {
        return sessionTypeRepository.findByTenantIdAndIsActive(tenantId, true);
    }

    public SessionType getSessionTypeById(UUID id, UUID tenantId) {
        return sessionTypeRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new RuntimeException("Session type not found"));
    }

    @Transactional
    public SessionType createSessionType(UUID tenantId, SessionTypeRequest request) {
        SessionType sessionType = new SessionType();
        sessionType.setTenantId(tenantId);
        sessionType.setName(request.getName());
        sessionType.setDescription(request.getDescription());
        sessionType.setDurationMinutes(request.getDurationMinutes());
        sessionType.setPrice(request.getPrice());
        sessionType.setCurrency(request.getCurrency());
        sessionType.setCapacity(request.getCapacity());
        sessionType.setCategory(request.getCategory());
        sessionType.setColor(request.getColor());
        sessionType.setCancellationPolicy(request.getCancellationPolicy());
        sessionType.setActive(true);

        return sessionTypeRepository.save(sessionType);
    }

    @Transactional
    public SessionType updateSessionType(UUID id, UUID tenantId, SessionTypeRequest request) {
        SessionType sessionType = getSessionTypeById(id, tenantId);
        sessionType.setName(request.getName());
        sessionType.setDescription(request.getDescription());
        sessionType.setDurationMinutes(request.getDurationMinutes());
        sessionType.setPrice(request.getPrice());
        sessionType.setCurrency(request.getCurrency());
        sessionType.setCapacity(request.getCapacity());
        sessionType.setCategory(request.getCategory());
        sessionType.setColor(request.getColor());
        sessionType.setCancellationPolicy(request.getCancellationPolicy());

        return sessionTypeRepository.save(sessionType);
    }

    @Transactional
    public void deleteSessionType(UUID id, UUID tenantId) {
        SessionType sessionType = getSessionTypeById(id, tenantId);
        sessionType.setActive(false);
        sessionTypeRepository.save(sessionType);
    }
}
