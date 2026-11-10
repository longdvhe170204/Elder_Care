package com.eldercare.backend.incident.internal.incident.service;

import com.eldercare.backend.incident.api.events.IncidentRecorded;
import com.eldercare.backend.resident.api.ReassessmentApi;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Báo đánh giá lại cho 001 khi sự cố cần đánh giá lại (FR-043; 001 FR-024, research H-11). Chiều gọi incident → resident:
 * 001 không nghe {@code IncidentRecorded} vì tạo vòng module. Nghe đồng bộ cùng transaction ở ba chỗ đã phát sự kiện (ghi
 * sự cố, nâng mức lên Trung bình, đổi loại sang Ngã); 001 tự bỏ qua lần gọi lại cùng sự cố.
 */
@Component
class IncidentReassessmentNotice {

    private final ReassessmentApi reassessments;

    IncidentReassessmentNotice(ReassessmentApi reassessments) {
        this.reassessments = reassessments;
    }

    @EventListener
    void on(IncidentRecorded event) {
        if (event.needsReassessment()) {
            reassessments.notifyAfterIncident(event.residentId(), event.incidentId());
        }
    }
}
