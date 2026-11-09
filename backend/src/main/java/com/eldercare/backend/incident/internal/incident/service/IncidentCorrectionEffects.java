package com.eldercare.backend.incident.internal.incident.service;

import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.incident.api.events.EffectiveImpactsRemain;
import com.eldercare.backend.incident.api.events.IncidentRecorded;
import com.eldercare.backend.incident.internal.governance.domain.ResidentItemClosed;
import com.eldercare.backend.incident.internal.incident.domain.IncidentView;
import com.eldercare.backend.incident.internal.incident.persistence.FallMonitoringStore;
import com.eldercare.backend.incident.internal.incident.persistence.IncidentStore;
import com.eldercare.backend.incident.internal.infection.persistence.ContactListStore;
import com.eldercare.backend.incident.internal.infection.persistence.ContainmentStore;
import java.util.ArrayList;
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

/**
 * Tác động khi sự cố bị Hủy ghi nhận hoặc đổi loại (FR-046a, research S-11): không thu hồi tác động cũ, báo danh sách tác
 * động còn hiệu lực cho Điều dưỡng phụ trách và các Bác sĩ; đổi sang Ngã, lây nhiễm nghi ngờ thì áp tác động loại mới tại
 * thời điểm đính chính.
 */
@Service
public class IncidentCorrectionEffects {
    /** Lệnh tương ứng mỗi tác động (api-007), để người nhận tự quyết. */
    public static final String END_POST_FALL_MONITORING = "end-post-fall-monitoring";

    public static final String REMOVE_FROM_CONTACT_LIST = "contact-list";
    public static final String LIFT_ISOLATION = "isolations/lift";

    private static final String FALL = "FALL";
    private static final String SUSPECTED_INFECTION = "SUSPECTED_INFECTION";

    private final IncidentRecordService records;
    private final IncidentStore incidents;
    private final FallMonitoringStore fallMonitoring;
    private final ContactListStore contactLists;
    private final ContainmentStore containments;
    private final ApplicationEventPublisher events;
    private final BusinessTime time;

    public IncidentCorrectionEffects(
            IncidentRecordService records,
            IncidentStore incidents,
            FallMonitoringStore fallMonitoring,
            ContactListStore contactLists,
            ContainmentStore containments,
            ApplicationEventPublisher events,
            BusinessTime time) {
        this.records = records;
        this.incidents = incidents;
        this.fallMonitoring = fallMonitoring;
        this.contactLists = contactLists;
        this.containments = containments;
        this.events = events;
        this.time = time;
    }

    /** Hủy ghi nhận: báo các tác động còn hiệu lực của loại hiện hành. */
    public void voided(IncidentView incident, long correctionId) {
        notifyRemaining(incident, incidents.currentTypeCode(incident.id()), correctionId);
        if (incident.residentId() != null) events.publishEvent(new ResidentItemClosed(incident.residentId()));
    }

    /** Đổi loại: loại cũ xử lý như Hủy; loại mới là Ngã, lây nhiễm nghi ngờ thì áp tác động từ bây giờ. */
    public void typeChanged(
            IncidentView incident, String oldTypeCode, String newTypeCode, long actor, long correctionId) {
        notifyRemaining(incident, oldTypeCode, correctionId);
        Long residentId = incident.residentId();
        if (residentId == null || newTypeCode.equals(oldTypeCode)) return;
        var now = time.now();
        if (FALL.equals(newTypeCode)) {
            events.publishEvent(new IncidentRecorded(
                    incident.id(), residentId, FALL, incident.level().name(), true));
            records.startFallMonitoring(incident.id(), residentId, incident.floorAreaId(), now, now, actor);
        } else if (SUSPECTED_INFECTION.equals(newTypeCode)) {
            records.startInfectionSuspicion(incident.id(), residentId, incident.floorAreaId(), now, actor);
        }
    }

    /** Danh sách tác động còn hiệu lực của sự cố theo loại cũ (FR-046a). */
    public List<String> remainingImpacts(IncidentView incident, String typeCode) {
        var impacts = new ArrayList<String>();
        if (FALL.equals(typeCode) && incident.residentId() != null) {
            fallMonitoring
                    .active(incident.residentId())
                    .filter(row -> row.content().incidentId() == incident.id())
                    .ifPresent(row -> impacts.add(END_POST_FALL_MONITORING));
        }
        if (SUSPECTED_INFECTION.equals(typeCode)) {
            if (contactLists.findByIncident(incident.id()).isPresent()) impacts.add(REMOVE_FROM_CONTACT_LIST);
            if (containments.activeFor(incident.id())) impacts.add(LIFT_ISOLATION);
        }
        return impacts;
    }

    private void notifyRemaining(IncidentView incident, String typeCode, long correctionId) {
        var impacts = remainingImpacts(incident, typeCode);
        if (impacts.isEmpty()) return;
        var now = time.now();
        events.publishEvent(new EffectiveImpactsRemain(
                incident.id(),
                impacts,
                "M05.INCIDENT:" + incident.id() + ":EFFECTIVE_IMPACTS_REMAIN:" + correctionId,
                now,
                IncidentNotices.effectiveImpacts(
                        incident.id(), incident.residentId(), incident.floorAreaId(), correctionId, impacts, now)));
    }
}
