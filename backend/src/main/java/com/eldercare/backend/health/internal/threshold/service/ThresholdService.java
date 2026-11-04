package com.eldercare.backend.health.internal.threshold.service;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.RequiresPermission;
import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.administration.auditlog.api.dto.AuditAction;
import com.eldercare.backend.administration.auditlog.api.dto.AuditLogEntry;
import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.data.BusinessCommand;
import com.eldercare.backend.common.data.TechnicalLocks;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.error.ConditionNotMetException;
import com.eldercare.backend.common.error.InvalidValueException;
import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.common.error.ReasonRequiredException;
import com.eldercare.backend.common.error.UnmetCondition;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.health.internal.governance.service.VitalSignLock;
import com.eldercare.backend.health.internal.threshold.domain.NewThreshold;
import com.eldercare.backend.health.internal.threshold.domain.ThresholdActions;
import com.eldercare.backend.health.internal.threshold.domain.ThresholdRules;
import com.eldercare.backend.health.internal.threshold.domain.ThresholdVersion;
import com.eldercare.backend.health.internal.threshold.persistence.ThresholdVersionStore;
import com.eldercare.backend.resident.api.ResidentApi;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lệnh ngưỡng của Bác sĩ theo bảng FR-022 (research S-07): thiết lập, hủy phiên bản Chờ hiệu lực, ngừng ngưỡng cá
 * nhân; việc theo phút chuyển Chờ hiệu lực → Hiệu lực. Khóa {@code VitalSignLock} của người cao tuổi với ngưỡng cá
 * nhân, khóa kỹ thuật theo chỉ số với ngưỡng mặc định.
 */
@Service
public class ThresholdService {
    public static final String FACILITY = "FACILITY";
    public static final String PERSONAL = "PERSONAL";
    private static final String ENTITY = "M06.THRESHOLD";

    private final ThresholdVersionStore store;
    private final ThresholdIndicators indicators;
    private final ResidentApi residents;
    private final VitalSignLock residentLock;
    private final TechnicalLocks locks;
    private final AuditLogApi audit;
    private final BusinessTime time;
    private final MissingThresholdReminders reminders;

    public ThresholdService(
            ThresholdVersionStore store,
            ThresholdIndicators indicators,
            ResidentApi residents,
            VitalSignLock residentLock,
            TechnicalLocks locks,
            AuditLogApi audit,
            BusinessTime time,
            MissingThresholdReminders reminders) {
        this.store = store;
        this.indicators = indicators;
        this.residents = residents;
        this.residentLock = residentLock;
        this.locks = locks;
        this.audit = audit;
        this.time = time;
        this.reminders = reminders;
    }

    @RequiresPermission(function = Function.ALERT_THRESHOLD, operation = Operation.CREATE)
    @Transactional
    public long set(NewThreshold input, CurrentUser user) {
        boolean personal = PERSONAL.equals(input.scope());
        if (!personal && !FACILITY.equals(input.scope()))
            throw new InvalidValueException("Phạm vi ngưỡng không hợp lệ");
        if (personal == (input.residentId() == null)) {
            throw new InvalidValueException("Ngưỡng cá nhân phải có người cao tuổi, ngưỡng mặc định thì không");
        }
        if (input.effectiveFrom() == null) throw new InvalidValueException("Thiếu thời điểm hiệu lực");
        var indicator = indicators.require(input.indicatorId());
        if (indicator.validRange() == null) {
            throw new BusinessException(
                    HttpStatus.CONFLICT, "VALID_RANGE_MISSING", "Chỉ số chưa có khoảng hợp lệ vật lý (CFG-M06-03)");
        }
        var violations =
                ThresholdRules.violations(input.components(), indicator.componentCodes(), indicator.validRange());
        if (!violations.isEmpty()) {
            throw new BusinessException(
                    HttpStatus.UNPROCESSABLE_CONTENT,
                    "INVALID_THRESHOLD",
                    "Ngưỡng không hợp lệ",
                    Map.of("violations", violations));
        }
        var now = time.now();
        var requested = input.effectiveFrom().truncatedTo(ChronoUnit.MINUTES);
        var commandMinute = now.truncatedTo(ChronoUnit.MINUTES);
        if (requested.isBefore(commandMinute)) throw blocked("Thời điểm hiệu lực không được trước thời điểm lệnh");
        boolean immediate = !requested.isAfter(commandMinute);
        var effectiveFrom = immediate ? now : input.effectiveFrom();

        if (personal) {
            if (residents.isFinalStatus(input.residentId())) throw blocked("Người cao tuổi đã ở trạng thái cuối");
            residentLock.lock(input.residentId());
        } else {
            locks.lock(ENTITY + ":" + FACILITY + ":" + input.indicatorId());
        }
        var open = store.open(input.scope(), input.residentId(), input.indicatorId());
        if (!open.isEmpty() && input.reason() == null) throw new ReasonRequiredException();
        String reason = input.reason() == null ? null : input.reason().value();
        for (var current : open) {
            if ("PENDING".equals(current.status())) {
                store.end(current.id(), "CANCELLED", null, reason, user.accountId(), current.version());
            } else if (immediate) {
                store.end(current.id(), "EXPIRED", effectiveFrom, null, null, current.version());
            }
        }
        long id = store.insert(
                new ThresholdVersion(
                        0,
                        input.scope(),
                        input.residentId(),
                        input.indicatorId(),
                        effectiveFrom,
                        null,
                        immediate ? "EFFECTIVE" : "PENDING",
                        input.components(),
                        0),
                reason,
                user.accountId(),
                now);
        var after = new HashMap<String, Object>();
        after.put("scope", input.scope());
        after.put("indicatorId", input.indicatorId());
        after.put("status", immediate ? "EFFECTIVE" : "PENDING");
        after.put("effectiveFrom", effectiveFrom.toString());
        write(ThresholdActions.SET, id, input.residentId(), after, input.reason());
        return id;
    }

    /** Chờ hiệu lực → Đã hủy, bắt buộc lý do. */
    @BusinessCommand
    @RequiresPermission(function = Function.ALERT_THRESHOLD, operation = Operation.UPDATE)
    @Transactional
    public void cancel(long id, Reason reason, CurrentUser user) {
        var version = store.lock(id).orElseThrow(NotFoundException::new);
        if (!"PENDING".equals(version.status())) throw blocked("Chỉ hủy được phiên bản Chờ hiệu lực");
        store.end(id, "CANCELLED", null, reason.value(), user.accountId(), version.version());
        write(ThresholdActions.CANCELLED, id, version.residentId(), Map.of("status", "CANCELLED"), reason);
    }

    /** Ngừng ngưỡng cá nhân Hiệu lực, bắt buộc lý do; từ đó so theo ngưỡng mặc định. */
    @BusinessCommand
    @RequiresPermission(function = Function.ALERT_THRESHOLD, operation = Operation.UPDATE)
    @Transactional
    public void stop(long id, Reason reason, CurrentUser user) {
        var version = store.lock(id).orElseThrow(NotFoundException::new);
        if (!PERSONAL.equals(version.scope()) || !"EFFECTIVE".equals(version.status())) {
            throw blocked("Chỉ ngừng được ngưỡng cá nhân đang Hiệu lực");
        }
        store.end(id, "EXPIRED", time.now(), reason.value(), user.accountId(), version.version());
        write(ThresholdActions.STOPPED, id, version.residentId(), Map.of("status", "EXPIRED"), reason);
        reminders.allowReminderAgain(version.residentId());
    }

    /** Việc theo phút: phiên bản Chờ hiệu lực tới hạn thành Hiệu lực, phiên bản Hiệu lực cũ hết hiệu lực tại thời điểm đó. */
    @Transactional
    public int activateDue(Instant at) {
        int done = 0;
        for (var due : store.due(at)) {
            if (due.residentId() != null) {
                residentLock.lock(due.residentId());
            } else {
                locks.lock(ENTITY + ":" + FACILITY + ":" + due.indicatorId());
            }
            var pending = store.lock(due.id()).orElse(null);
            if (pending == null || !"PENDING".equals(pending.status())) continue;
            for (var current : store.open(pending.scope(), pending.residentId(), pending.indicatorId())) {
                if (current.id() != pending.id() && "EFFECTIVE".equals(current.status())) {
                    store.end(current.id(), "EXPIRED", pending.effectiveFrom(), null, null, current.version());
                }
            }
            store.activate(pending.id(), pending.version());
            done++;
        }
        return done;
    }

    private void write(AuditAction action, long id, Long residentId, Map<String, Object> after, Reason reason) {
        var entry = AuditLogEntry.builder()
                .action(action)
                .entity(ENTITY, id)
                .resident(residentId)
                .after(after);
        if (reason != null) entry.reason(reason);
        audit.write(entry.build());
    }

    private static ConditionNotMetException blocked(String message) {
        return new ConditionNotMetException(List.of(new UnmetCondition("FR-022", message)));
    }
}
