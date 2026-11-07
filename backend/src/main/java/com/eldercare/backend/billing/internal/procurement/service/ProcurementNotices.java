package com.eldercare.backend.billing.internal.procurement.service;

import com.eldercare.backend.billing.internal.procurement.domain.DepartmentLeadMissing;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.notification.*;
import com.eldercare.backend.common.time.BusinessTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 019 FR-021: thông báo mức Nhẹ của yêu cầu mua sắm; nội dung không có thông tin sức khỏe, không gắn người cao tuổi.
 * Module chỉ dựng khuôn, 009 gửi (B-02).
 */
@Component
public class ProcurementNotices {
    static final String MODULE = "M11";
    static final String CREATOR = "PROCUREMENT_REQUEST_CREATOR";
    static final String RECONCILER = "PROCUREMENT_RECONCILER";

    private final BusinessTime time;
    private final ApplicationEventPublisher events;

    public ProcurementNotices(BusinessTime time, ApplicationEventPublisher events) {
        this.time = time;
        this.events = events;
    }

    static RecipientGroup manager() {
        return new RecipientGroup.FacilityRole(Role.NURSING_HOME_MANAGER);
    }

    static RecipientGroup accountant() {
        return new RecipientGroup.FacilityRole(Role.ACCOUNTANT);
    }

    static RecipientGroup creator(long staffId) {
        return new RecipientGroup.Staff(staffId, CREATOR);
    }

    static RecipientGroup reconciler(long staffId) {
        return new RecipientGroup.Staff(staffId, RECONCILER);
    }

    List<NotificationRequest> request(
            String event, long requestId, String eventKey, String title, List<RecipientGroup> recipients) {
        if (recipients.isEmpty()) return List.of();
        var now = time.now();
        return List.of(new NotificationRequest(
                MODULE,
                event,
                SourceGroup.OTHER,
                new SourceRecord(ProcurementRequestService.ENTITY, Long.toString(requestId)),
                event + ":" + requestId + ":" + eventKey,
                Optional.of(NotificationLevel.LOW),
                Optional.empty(),
                Optional.empty(),
                List.copyOf(recipients),
                List.of(new ContentPart(InformationType.GENERAL, title)),
                Optional.of(title),
                Optional.of(title),
                Optional.empty(),
                false,
                now,
                Map.of("requestId", requestId),
                false,
                Optional.empty(),
                Optional.empty()));
    }

    /** Q-292: bộ phận chưa có Phụ trách bộ phận; nhắc trong transaction riêng vì lệnh gọi bị chặn và rollback. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void departmentLeadMissing(long departmentId) {
        var now = time.now();
        String title = "Bộ phận chưa có Phụ trách bộ phận: lệnh mua sắm bị chặn, cần chỉ định Phụ trách";
        events.publishEvent(new DepartmentLeadMissing(
                departmentId,
                List.of(new NotificationRequest(
                        MODULE,
                        "PROCUREMENT_DEPARTMENT_LEAD_MISSING",
                        SourceGroup.OTHER,
                        new SourceRecord("M09.DEPARTMENT", Long.toString(departmentId)),
                        "PROCUREMENT_DEPARTMENT_LEAD_MISSING:" + departmentId + ":" + now,
                        Optional.of(NotificationLevel.LOW),
                        Optional.empty(),
                        Optional.empty(),
                        List.of(manager()),
                        List.of(new ContentPart(InformationType.GENERAL, title)),
                        Optional.of(title),
                        Optional.of(title),
                        Optional.empty(),
                        false,
                        now,
                        Map.of("departmentId", departmentId),
                        false,
                        Optional.empty(),
                        Optional.empty()))));
    }
}
