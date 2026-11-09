package com.eldercare.backend.incident.internal.incident.service;

import com.eldercare.backend.administration.parameter.api.ParameterApi;
import com.eldercare.backend.administration.parameter.api.dto.ParameterCode;
import com.eldercare.backend.incident.api.events.AlertEscalated;
import com.eldercare.backend.incident.api.events.MaximumEscalationReminder;
import com.eldercare.backend.incident.api.spi.AlertStaffPort;
import com.eldercare.backend.incident.internal.alert.domain.AlertEscalationChain;
import com.eldercare.backend.incident.internal.alert.domain.IncidentLevel;
import com.eldercare.backend.incident.internal.alert.service.AcknowledgementDeadlines;
import com.eldercare.backend.incident.internal.incident.domain.IncidentEscalationState;
import com.eldercare.backend.incident.internal.incident.persistence.IncidentStore;
import java.time.Instant;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Leo thang sự cố theo phút (FR-044; research S-11 "cùng job và chuỗi của S-08"): sự cố Mới quá hạn tiếp nhận lên một
 * cấp mỗi lần, bỏ qua cấp không có người; trạng thái giữ Mới (CHK022); cấp 2 vẫn quá hạn thì mang dấu tối đa rồi nhắc
 * mỗi khoảng một lần (FR-034). "Tiếp nhận xử lý" đặt {@code escalation_stopped} nên dừng.
 */
@Service
public class IncidentEscalationService {
    private static final int BATCH = 200;
    private static final Logger log = LoggerFactory.getLogger(IncidentEscalationService.class);

    private final IncidentStore incidents;
    private final ApplicationEventPublisher events;
    private final ParameterApi parameters;
    private final TransactionTemplate transaction;
    private final Optional<AlertStaffPort> staff;

    public IncidentEscalationService(
            IncidentStore incidents,
            ApplicationEventPublisher events,
            ParameterApi parameters,
            TransactionTemplate transaction,
            @Autowired(required = false) AlertStaffPort staff) {
        this.incidents = incidents;
        this.events = events;
        this.parameters = parameters;
        this.transaction = transaction;
        this.staff = Optional.ofNullable(staff);
    }

    /** Số sự cố đã xử lý; lỗi của một sự cố không chặn sự cố khác. */
    public int run(Instant now) {
        int processed = 0;
        for (long id : incidents.dueForEscalation(now, BATCH)) {
            try {
                if (escalate(id, now)) processed++;
            } catch (RuntimeException exception) {
                log.warn("Leo thang sự cố {} lỗi", id, exception);
            }
        }
        return processed;
    }

    /** Xử lý một sự cố trong transaction riêng; false khi chưa tới hạn hoặc đã dừng. */
    public boolean escalate(long incidentId, Instant now) {
        return Boolean.TRUE.equals(transaction.execute(status -> escalateOne(incidentId, now)));
    }

    private boolean escalateOne(long id, Instant now) {
        IncidentEscalationState incident = incidents.lockForEscalation(id).orElse(null);
        if (incident == null
                || !"NEW".equals(incident.status())
                || incident.escalationStopped()
                || incident.acknowledgementDeadline() == null
                || incident.acknowledgementDeadline().isAfter(now)) {
            return false;
        }
        if (AlertEscalationChain.atMaximum(incident.escalationLevel()) || incident.maxEscalationAt() != null) {
            if (incident.maxEscalationAt() == null) {
                Instant at = incident.acknowledgementDeadline();
                incidents.reachMaximum(id, at, nextReminder(incident, at, now), incident.version());
            } else {
                int sequence = incident.maxReminderCount() + 1;
                incidents.reminded(
                        id,
                        sequence,
                        nextReminder(incident, incident.acknowledgementDeadline(), now),
                        incident.version());
                events.publishEvent(new MaximumEscalationReminder(
                        "M05.INCIDENT",
                        id,
                        sequence,
                        "M05.INCIDENT:" + id + ":MAX_REMINDER:" + sequence,
                        now,
                        IncidentNotices.maximumReminder(
                                id, incident.residentId(), incident.floorAreaId(), sequence, now)));
            }
            return true;
        }
        var next = AlertEscalationChain.next(incident.escalationLevel(), level -> hasRecipients(incident, level, now));
        if (next.isEmpty()) {
            Instant at = incident.acknowledgementDeadline();
            incidents.reachMaximum(id, at, nextReminder(incident, at, now), incident.version());
            return true;
        }
        int level = next.getAsInt();
        incidents.escalate(id, level, deadline(incident, now), incident.version());
        events.publishEvent(new AlertEscalated(
                "M05.INCIDENT",
                id,
                level,
                "M05.INCIDENT:" + id + ":ESCALATED:" + level,
                now,
                IncidentNotices.escalated(id, incident.residentId(), incident.floorAreaId(), level, now)));
        return true;
    }

    /** Cấp 1 cần Trưởng tầng được giao hoặc Bác sĩ trực; cấp 2 (Quản lý viện) luôn có. */
    private boolean hasRecipients(IncidentEscalationState incident, int level, Instant now) {
        if (level >= AlertEscalationChain.MAX_LEVEL) return true;
        if (staff.isEmpty()) return false;
        var port = staff.get();
        boolean floorManager = incident.floorAreaId() != null
                && port.assignedFloorManager(incident.floorAreaId(), now).isPresent();
        return floorManager || !port.onDutyDoctors(now).isEmpty();
    }

    private Instant deadline(IncidentEscalationState incident, Instant now) {
        return AcknowledgementDeadlines.of(incident.level(), now, incident.floorAreaId(), parameters, staff);
    }

    /** Như cảnh báo: Trung bình nhắc theo mốc cũ + k × CFG-M05-01 (không trôi); Nhẹ khi hết ca (FR-034, FR-084 d). */
    private Instant nextReminder(IncidentEscalationState incident, Instant base, Instant now) {
        if (incident.level() == IncidentLevel.MEDIUM) {
            var interval = parameters.read(ParameterCode.CFG_M05_01).asDuration();
            Instant next = base.plus(interval);
            while (!next.isAfter(now)) next = next.plus(interval);
            return next;
        }
        return deadline(incident, now);
    }
}
