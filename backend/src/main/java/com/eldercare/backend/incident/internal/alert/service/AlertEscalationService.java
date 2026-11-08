package com.eldercare.backend.incident.internal.alert.service;

import com.eldercare.backend.administration.parameter.api.ParameterApi;
import com.eldercare.backend.administration.parameter.api.dto.ParameterCode;
import com.eldercare.backend.administration.shared.api.RecordEventApi;
import com.eldercare.backend.incident.api.events.AlertEscalated;
import com.eldercare.backend.incident.api.events.MaximumEscalationReminder;
import com.eldercare.backend.incident.api.spi.AlertStaffPort;
import com.eldercare.backend.incident.internal.alert.domain.AlertEscalationChain;
import com.eldercare.backend.incident.internal.alert.domain.AlertRecord;
import com.eldercare.backend.incident.internal.alert.domain.AlertStatus;
import com.eldercare.backend.incident.internal.alert.domain.IncidentLevel;
import com.eldercare.backend.incident.internal.alert.persistence.AlertStore;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Leo thang cảnh báo theo phút (FR-033, FR-034, FR-084 a, d; research S-08, S-21): mỗi lần chạy mỗi cảnh báo quá hạn
 * chỉ lên một cấp, bỏ qua cấp không có người; cấp 2 vẫn quá hạn thì mang dấu "đã leo thang tối đa" rồi nhắc mỗi khoảng
 * một lần (khóa sự kiện theo số thứ tự nên chạy lại không gửi trùng).
 */
@Service
public class AlertEscalationService {
    private static final int BATCH = 200;
    private static final Logger log = LoggerFactory.getLogger(AlertEscalationService.class);

    private final AlertStore alerts;
    private final ApplicationEventPublisher events;
    private final ParameterApi parameters;
    private final TransactionTemplate transaction;
    private final Optional<AlertStaffPort> staff;
    private final AlertHistory history;

    public AlertEscalationService(
            AlertStore alerts,
            ApplicationEventPublisher events,
            ParameterApi parameters,
            TransactionTemplate transaction,
            @Autowired(required = false) AlertStaffPort staff,
            @Autowired(required = false) RecordEventApi records) {
        this.alerts = alerts;
        this.events = events;
        this.parameters = parameters;
        this.transaction = transaction;
        this.staff = Optional.ofNullable(staff);
        this.history = new AlertHistory(records);
    }

    /** Số cảnh báo đã xử lý (lên cấp, đánh dấu tối đa hoặc nhắc); lỗi của một cảnh báo không chặn cảnh báo khác. */
    public int run(Instant now) {
        int processed = 0;
        for (long id : alerts.dueForEscalation(now, BATCH)) {
            try {
                if (escalate(id, now)) processed++;
            } catch (RuntimeException exception) {
                log.warn("Leo thang cảnh báo {} lỗi", id, exception);
            }
        }
        return processed;
    }

    /** Xử lý một cảnh báo trong transaction riêng; false khi chưa tới hạn hoặc đã dừng. */
    public boolean escalate(long alertId, Instant now) {
        return Boolean.TRUE.equals(transaction.execute(status -> escalateOne(alertId, now)));
    }

    private boolean escalateOne(long id, Instant now) {
        AlertRecord alert = alerts.lock(id).orElse(null);
        if (alert == null
                || (alert.status() != AlertStatus.NEW && alert.status() != AlertStatus.ESCALATED)
                || alert.escalationStopped()
                || alert.acknowledgementDeadline() == null
                || alert.acknowledgementDeadline().isAfter(now)) {
            return false;
        }
        if (AlertEscalationChain.atMaximum(alert.escalationLevel()) || alert.maxEscalationAt() != null) {
            if (alert.maxEscalationAt() == null) {
                reachMaximum(alert, now);
            } else {
                remind(alert, now);
            }
            return true;
        }
        var next = AlertEscalationChain.next(alert.escalationLevel(), level -> hasRecipients(alert, level, now));
        if (next.isEmpty()) {
            reachMaximum(alert, now);
            return true;
        }
        int level = next.getAsInt();
        Instant deadline = deadline(alert.level(), alert, now);
        alerts.escalate(alert.id(), level, deadline, alert.version());
        history.add(
                alert.id(),
                "ESCALATED",
                Map.of(
                        "from",
                        alert.escalationLevel(),
                        "to",
                        level,
                        "recipients",
                        AlertNotices.levelGroups(level, alert.floorAreaId()).stream()
                                .map(group -> group.getClass().getSimpleName())
                                .toList()),
                now);
        events.publishEvent(new AlertEscalated(
                alert.id(),
                level,
                AlertNotices.SOURCE_TYPE + ":" + alert.id() + ":ESCALATED:" + level,
                now,
                AlertNotices.escalated(alert, level, now)));
        return true;
    }

    private void reachMaximum(AlertRecord alert, Instant now) {
        Instant at = alert.acknowledgementDeadline();
        alerts.reachMaximum(alert.id(), at, nextReminder(alert, at, now), alert.version());
        history.add(alert.id(), "REACHED_MAXIMUM", Map.of("level", alert.escalationLevel()), now);
    }

    private void remind(AlertRecord alert, Instant now) {
        int sequence = alert.maxReminderCount() + 1;
        alerts.reminded(
                alert.id(), sequence, nextReminder(alert, alert.acknowledgementDeadline(), now), alert.version());
        events.publishEvent(new MaximumEscalationReminder(
                alert.id(),
                sequence,
                AlertNotices.SOURCE_TYPE + ":" + alert.id() + ":MAX_REMINDER:" + sequence,
                now,
                AlertNotices.maximumReminder(alert, sequence, now)));
    }

    /** Cấp 1 cần Trưởng tầng được giao hoặc Bác sĩ trực; cấp 2 (Quản lý viện) luôn có. */
    private boolean hasRecipients(AlertRecord alert, int level, Instant now) {
        if (level >= AlertEscalationChain.MAX_LEVEL) return true;
        if (staff.isEmpty()) return false;
        var port = staff.get();
        boolean floorManager = alert.floorAreaId() != null
                && port.assignedFloorManager(alert.floorAreaId(), now).isPresent();
        return floorManager || !port.onDutyDoctors(now).isEmpty();
    }

    /** Hạn mới sau khi lên cấp: Trung bình là bây giờ + CFG-M05-01, Nhẹ là hết ca (FR-031, FR-033). */
    private Instant deadline(IncidentLevel level, AlertRecord alert, Instant now) {
        return AcknowledgementDeadlines.of(level, now, alert.floorAreaId(), parameters, staff);
    }

    /**
     * Lần nhắc kế tiếp: mốc cũ + k × khoảng với Trung bình (không trôi), lấy mốc đầu tiên sau lúc chạy để lần nhắc bị
     * lỡ chỉ gửi một lần (FR-034, FR-084 d); hết ca của tầng với Nhẹ.
     */
    private Instant nextReminder(AlertRecord alert, Instant base, Instant now) {
        if (alert.level() == IncidentLevel.MEDIUM) {
            var interval = parameters.read(ParameterCode.CFG_M05_01).asDuration();
            Instant next = base.plus(interval);
            while (!next.isAfter(now)) next = next.plus(interval);
            return next;
        }
        return deadline(alert.level(), alert, now);
    }
}
