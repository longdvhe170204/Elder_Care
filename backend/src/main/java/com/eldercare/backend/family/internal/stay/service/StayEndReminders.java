package com.eldercare.backend.family.internal.stay.service;

import com.eldercare.backend.administration.shared.api.SimpleRecordApi;
import com.eldercare.backend.administration.shared.api.dto.SimpleRecord;
import com.eldercare.backend.family.api.events.FamilyStayNotice;
import com.eldercare.backend.family.internal.stay.domain.FamilyStay;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import com.eldercare.backend.resident.api.events.ResidentStatusChanged;
import java.util.Set;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Người cao tuổi chuyển Tạm vắng, Điều trị tại bệnh viện hoặc trạng thái cuối khi có lượt Đang ở lại: nhắc Hành chính,
 * Trưởng tầng kết thúc lượt; lượt không tự kết thúc (FR-044, Q-344).
 */
@Component
class StayEndReminders {

    private static final Set<ResidentStatus> AWAY = Set.of(
            ResidentStatus.TEMPORARILY_ABSENT,
            ResidentStatus.HOSPITALIZED,
            ResidentStatus.RESIDENCY_ENDED,
            ResidentStatus.DECEASED,
            ResidentStatus.ADMISSION_CANCELLED);

    private final SimpleRecordApi records;
    private final FamilyStayService stays;
    private final ApplicationEventPublisher events;

    StayEndReminders(SimpleRecordApi records, FamilyStayService stays, ApplicationEventPublisher events) {
        this.records = records;
        this.stays = stays;
        this.events = events;
    }

    @EventListener
    @Transactional
    public void residentStatusChanged(ResidentStatusChanged event) {
        if (!AWAY.contains(event.to())) {
            return;
        }
        for (SimpleRecord<FamilyStay> stay :
                records.listByResident(FamilyStay.KIND, event.residentId(), Set.of(FamilyStay.Status.STAYING.name()))) {
            events.publishEvent(stays.notice(stay, FamilyStayNotice.Kind.END_REMINDER, event.at()));
        }
    }
}
