package com.eldercare.backend.incident.internal.incident.service;

import com.eldercare.backend.incident.api.events.IncidentNotificationCreated;
import com.eldercare.backend.incident.internal.incident.persistence.IncidentStore;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import com.eldercare.backend.resident.api.events.ResidentStatusChanged;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Người cao tuổi Điều trị tại bệnh viện được Ghi nhận trở về (001): sự cố còn mở đã chuyển viện ghi diễn biến hệ thống
 * "Đã về viện" và báo người xử lý, trong cùng transaction lệnh trạng thái (đề xuất 2026-11-23 lượt 3, góp ý test demo:
 * sự cố không biết cụ đã về). Không đổi trạng thái sự cố.
 */
@Component
public class HospitalReturnListener {
    private final IncidentStore incidents;
    private final IncidentProgressionService progressions;
    private final ApplicationEventPublisher events;

    public HospitalReturnListener(
            IncidentStore incidents, IncidentProgressionService progressions, ApplicationEventPublisher events) {
        this.incidents = incidents;
        this.progressions = progressions;
        this.events = events;
    }

    @EventListener
    @Transactional
    public void on(ResidentStatusChanged event) {
        if (event.from() != ResidentStatus.HOSPITALIZED || event.to() != ResidentStatus.IN_RESIDENCE) return;
        for (long incidentId : incidents.openIds(event.residentId())) {
            var notices = progressions.addResidentReturned(incidentId, event.at());
            if (notices.isEmpty()) continue;
            events.publishEvent(new IncidentNotificationCreated(
                    incidentId, "LOW", notices.get(0).eventKey(), event.at(), notices));
        }
    }
}
