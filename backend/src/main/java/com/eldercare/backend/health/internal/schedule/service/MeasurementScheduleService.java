package com.eldercare.backend.health.internal.schedule.service;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.RequiresPermission;
import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.administration.auditlog.api.dto.AuditAction;
import com.eldercare.backend.administration.auditlog.api.dto.AuditLogEntry;
import com.eldercare.backend.care.api.TaskSourceApi;
import com.eldercare.backend.care.api.spi.TaskSourcePort.SourceType;
import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.data.BusinessCommand;
import com.eldercare.backend.common.error.ConditionNotMetException;
import com.eldercare.backend.common.error.InvalidValueException;
import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.common.error.UnmetCondition;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.health.api.events.MeasurementScheduleChanged;
import com.eldercare.backend.health.internal.governance.service.VitalSignLock;
import com.eldercare.backend.health.internal.schedule.domain.MeasurementSchedule;
import com.eldercare.backend.health.internal.schedule.domain.MeasurementTimeline;
import com.eldercare.backend.health.internal.schedule.domain.NewMeasurementSchedule;
import com.eldercare.backend.health.internal.schedule.persistence.MeasurementScheduleStore;
import com.eldercare.backend.health.internal.threshold.service.ThresholdIndicators;
import com.eldercare.backend.resident.api.ResidentApi;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lịch đo theo bảng FR-006 (research S-05): tạo, thay đổi (lịch mới trỏ về lịch cũ, cùng lệnh), ngừng; việc theo phút
 * chuyển Chờ hiệu lực → Hiệu lực, Hiệu lực → Hết hạn; trạng thái cuối của người cao tuổi ngừng lịch. Lịch đo chỉ là căn
 * cứ lập công việc của 005, không sinh công việc (FR-004, Q-306). Quyền theo dòng "Ngưỡng cảnh báo" của Phụ lục 27
 * (research S-05, Q-279).
 */
@Service
public class MeasurementScheduleService {
    private static final String ENTITY = "M06.MEASUREMENT_SCHEDULE";
    private static final Set<String> PRIORITIES = Set.of("NORMAL", "IMPORTANT", "MANDATORY");
    private static final Set<String> OPEN = Set.of("PENDING", "EFFECTIVE");
    private static final AuditAction CREATED = new AuditAction("MEASUREMENT_SCHEDULE_CREATED", false);
    private static final AuditAction CHANGED = new AuditAction("MEASUREMENT_SCHEDULE_CHANGED", true);
    private static final AuditAction STOPPED = new AuditAction("MEASUREMENT_SCHEDULE_STOPPED", true);
    static final String FINAL_STATUS_REASON = "Người cao tuổi chuyển trạng thái cuối";

    private final MeasurementScheduleStore store;
    private final ThresholdIndicators indicators;
    private final ResidentApi residents;
    private final VitalSignLock lock;
    private final TaskSourceApi taskSources;
    private final AuditLogApi audit;
    private final ApplicationEventPublisher events;
    private final BusinessTime time;

    public MeasurementScheduleService(
            MeasurementScheduleStore store,
            ThresholdIndicators indicators,
            ResidentApi residents,
            VitalSignLock lock,
            TaskSourceApi taskSources,
            AuditLogApi audit,
            ApplicationEventPublisher events,
            BusinessTime time) {
        this.store = store;
        this.indicators = indicators;
        this.residents = residents;
        this.lock = lock;
        this.taskSources = taskSources;
        this.audit = audit;
        this.events = events;
        this.time = time;
    }

    @RequiresPermission(function = Function.ALERT_THRESHOLD, operation = Operation.CREATE)
    @Transactional
    public long create(long residentId, NewMeasurementSchedule input, CurrentUser user) {
        validate(input);
        if (residents.isFinalStatus(residentId)) throw blocked("Người cao tuổi đã ở trạng thái cuối");
        lock.lock(residentId);
        var now = time.now();
        long id = insert(residentId, input, null, now, user);
        write(CREATED, id, residentId, Map.of("status", statusFor(input, now)), null);
        return id;
    }

    /** Thay đổi lịch đo (FR-005): lịch mới trỏ về lịch cũ; lịch cũ Đã ngừng tại thời điểm hiệu lực của lịch mới. */
    @BusinessCommand
    @RequiresPermission(function = Function.ALERT_THRESHOLD, operation = Operation.UPDATE)
    @Transactional
    public long change(long scheduleId, NewMeasurementSchedule input, Reason reason, CurrentUser user) {
        validate(input);
        var old = openSchedule(scheduleId);
        lock.lock(old.residentId());
        var now = time.now();
        var newEffectiveAt = input.startAt().isAfter(now) ? input.startAt() : now;
        store.stop(old.id(), newEffectiveAt, reason.value(), user.accountId(), old.version());
        long id = insert(old.residentId(), input, old.id(), now, user);
        taskSources.sourceChanged(SourceType.MEASUREMENT, old.id());
        publishChanged(old, now);
        write(CHANGED, old.id(), old.residentId(), Map.of("status", "STOPPED", "replacedBy", id), reason);
        return id;
    }

    /** Ngừng lịch đo: công việc đo Chưa đến hạn của lịch chuyển Hủy kèm lệnh (005 FR-022). */
    @BusinessCommand
    @RequiresPermission(function = Function.ALERT_THRESHOLD, operation = Operation.UPDATE)
    @Transactional
    public void stop(long scheduleId, Reason reason, CurrentUser user) {
        var schedule = openSchedule(scheduleId);
        lock.lock(schedule.residentId());
        var now = time.now();
        store.stop(schedule.id(), now, reason.value(), user.accountId(), schedule.version());
        taskSources.sourceCancelled(SourceType.MEASUREMENT, schedule.id(), reason);
        publishChanged(schedule, now);
        write(STOPPED, schedule.id(), schedule.residentId(), Map.of("status", "STOPPED"), reason);
    }

    /** Việc theo phút: Chờ hiệu lực tới thời điểm bắt đầu → Hiệu lực; Hiệu lực tới thời điểm kết thúc → Hết hạn. */
    @Transactional
    public int transitionDue(Instant at) {
        int done = 0;
        for (var schedule : store.duePending(at)) {
            store.activate(schedule.id(), schedule.version());
            done++;
        }
        for (var schedule : store.dueExpiry(at)) {
            store.expire(schedule.id(), schedule.version());
            done++;
        }
        return done;
    }

    /** Dòng cuối bảng FR-006: người cao tuổi chuyển trạng thái cuối thì lịch Đã ngừng, lý do là trạng thái cuối. */
    @Transactional
    public void stopForFinalStatus(long residentId, Instant at) {
        lock.lock(residentId);
        for (var schedule : store.openByResident(residentId)) {
            store.stop(schedule.id(), at, FINAL_STATUS_REASON, null, schedule.version());
        }
    }

    private MeasurementSchedule openSchedule(long id) {
        var schedule = store.lock(id).orElseThrow(NotFoundException::new);
        if (!OPEN.contains(schedule.status())) throw blocked("Lịch đo đã kết thúc");
        return schedule;
    }

    private long insert(long residentId, NewMeasurementSchedule input, Long replaces, Instant now, CurrentUser user) {
        return store.insert(
                new MeasurementSchedule(0, residentId, input, statusFor(input, now), replaces, null, null, 0),
                user.accountId(),
                now);
    }

    private static String statusFor(NewMeasurementSchedule input, Instant now) {
        return input.startAt().isAfter(now) ? "PENDING" : "EFFECTIVE";
    }

    private void validate(NewMeasurementSchedule input) {
        if (input.indicatorIds().isEmpty()) throw new InvalidValueException("Lịch đo cần ít nhất một chỉ số");
        if (input.startAt() == null) throw new InvalidValueException("Thiếu thời điểm bắt đầu");
        if (input.endAt() != null && !input.endAt().isAfter(input.startAt())) {
            throw new InvalidValueException("Thời điểm kết thúc phải sau thời điểm bắt đầu");
        }
        if (!PRIORITIES.contains(input.priority())) throw new InvalidValueException("Mức quan trọng không hợp lệ");
        MeasurementTimeline.validate(input.frequency());
        input.indicatorIds().forEach(indicators::require);
    }

    private void publishChanged(MeasurementSchedule schedule, Instant now) {
        events.publishEvent(new MeasurementScheduleChanged(
                schedule.id(), schedule.residentId(), ENTITY + ":" + schedule.id() + ":" + now.toEpochMilli(), now));
    }

    private void write(AuditAction action, long id, long residentId, Map<String, Object> after, Reason reason) {
        var entry = AuditLogEntry.builder()
                .action(action)
                .entity(ENTITY, id)
                .resident(residentId)
                .after(after);
        if (reason != null) entry.reason(reason);
        audit.write(entry.build());
    }

    private static ConditionNotMetException blocked(String message) {
        return new ConditionNotMetException(List.of(new UnmetCondition("FR-006", message)));
    }
}
