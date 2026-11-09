package com.eldercare.backend.nutrition.internal.serving.service;

import com.eldercare.backend.incident.api.events.AlertRequested;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Yêu cầu 007 tạo cảnh báo trong transaction riêng: cảnh báo vẫn được ghi dù lệnh xác nhận phục vụ bị chặn và hoàn tác
 * (FR-049; research M-10).
 */
@Component
class ServingAlerts {

    private final ApplicationEventPublisher events;

    ServingAlerts(ApplicationEventPublisher events) {
        this.events = events;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void request(AlertRequested alert) {
        events.publishEvent(alert);
    }
}
