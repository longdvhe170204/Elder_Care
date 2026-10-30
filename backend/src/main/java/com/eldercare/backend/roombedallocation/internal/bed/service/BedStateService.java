package com.eldercare.backend.roombedallocation.internal.bed.service;

import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.administration.auditlog.api.dto.AuditAction;
import com.eldercare.backend.administration.auditlog.api.dto.AuditLogEntry;
import com.eldercare.backend.administration.parameter.api.ParameterApi;
import com.eldercare.backend.administration.parameter.api.dto.ParameterCode;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.error.ReasonRequiredException;
import com.eldercare.backend.common.notification.RecipientGroup;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.roombedallocation.api.dto.BedNotificationContext;
import com.eldercare.backend.roombedallocation.api.dto.BedStatus;
import com.eldercare.backend.roombedallocation.api.events.DelayedScheduledWork;
import com.eldercare.backend.roombedallocation.internal.bed.domain.BedEvent;
import com.eldercare.backend.roombedallocation.internal.bed.domain.BedHold;
import com.eldercare.backend.roombedallocation.internal.bed.domain.BedStateChange;
import com.eldercare.backend.roombedallocation.internal.bed.domain.BedTransition;
import com.eldercare.backend.roombedallocation.internal.bed.domain.BedTransitions;
import com.eldercare.backend.roombedallocation.internal.bed.domain.HoldReason;
import com.eldercare.backend.roombedallocation.internal.bed.persistence.BedStateStore;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** T008: cổng thay trạng thái duy nhất, trong transaction của lệnh nguồn đã kiểm quyền/điều kiện nghiệp vụ. */
@Service
public class BedStateService {
    private final BedStateStore store;
    private final ParameterApi parameters;
    private final BusinessTime time;
    private final AuditLogApi audit;
    private final ApplicationEventPublisher events;
    private final com.eldercare.backend.roombedallocation.internal.allocation.persistence.WaitingHoldIndexStore holds;

    public BedStateService(
            BedStateStore store,
            ParameterApi parameters,
            BusinessTime time,
            AuditLogApi audit,
            ApplicationEventPublisher events,
            com.eldercare.backend.roombedallocation.internal.allocation.persistence.WaitingHoldIndexStore holds) {
        this.store = store;
        this.parameters = parameters;
        this.time = time;
        this.audit = audit;
        this.events = events;
        this.holds = holds;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public BedStateChange transition(long bedId, BedTransition command) {
        var bed = store.lock(bedId);
        BedStatus before = bed.state();
        if (command.event() == BedEvent.CREATE_BED) {
            if (before != BedStatus.EMPTY || store.hasHistory(bedId)) throw invalid();
            before = null;
        }
        BedStatus maintenance = command.event() == BedEvent.ASSET_READY ? store.maintenanceSource(bedId) : null;
        BedStatus after;
        try {
            after = BedTransitions.next(
                    before, bed.holdReason(), command.event(), maintenance, command.startReservedAllocation());
        } catch (IllegalStateException error) {
            throw invalid();
        }
        if ((command.event() == BedEvent.ASSET_READY || command.event() == BedEvent.REUSE) && !bed.roomActive())
            throw invalid();
        BedHold hold = after == BedStatus.HELD ? command.hold() : null;
        if (after == BedStatus.HELD) {
            HoldReason required = command.event() == BedEvent.HOLD_WAITING_RECORD
                    ? HoldReason.WAITING_RECORD
                    : HoldReason.ABSENT_RESIDENT;
            if (hold == null || hold.reason() != required) throw invalid();
        }
        if (command.event() == BedEvent.CONFIRM_READY
                && (command.disinfectionRequired() == null
                        || (command.disinfectionRequired()
                                && (!Boolean.TRUE.equals(command.disinfected())
                                        || !Boolean.TRUE.equals(command.ppeUsed()))))) {
            throw new BusinessException(
                    HttpStatus.UNPROCESSABLE_CONTENT,
                    "DISINFECTION_CONFIRMATION_REQUIRED",
                    "Cần xác nhận khử khuẩn và đồ bảo hộ");
        }
        String suspension = null;
        if (after == BedStatus.SUSPENDED) {
            if (command.event() == BedEvent.ASSET_READY) suspension = store.suspensionReason(bedId);
            else {
                if (command.reason() == null) throw new ReasonRequiredException();
                suspension = command.reason().value();
            }
        }
        Instant now = time.now().truncatedTo(ChronoUnit.MICROS);
        Instant deadline = after == BedStatus.AWAITING_READY
                ? now.plus(parameters.read(ParameterCode.CFG_M03_04).asDuration())
                : null;
        boolean late = command.plannedAt() != null
                && Duration.between(command.plannedAt(), now)
                                .compareTo(parameters
                                        .read(ParameterCode.CFG_M03_08)
                                        .asDuration())
                        > 0;
        BedStateChange change = new BedStateChange(before, after, now, late);
        if (bed.holdReason() == HoldReason.WAITING_RECORD && bed.holdResidentId() != null)
            holds.update(bed.holdResidentId(), bedId, false);
        if (hold != null && hold.reason() == HoldReason.WAITING_RECORD) holds.update(hold.residentId(), bedId, true);
        store.writeState(bedId, after, hold, now, deadline, command.closedAllocationId(), suspension);
        store.appendHistory(bed, change, command);
        Map<String, Object> afterValues = new LinkedHashMap<>();
        afterValues.put("state", after.name());
        if (command.healthReason() && command.reason() != null)
            afterValues.put("infectionControlReason", command.reason().value());
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("event", command.event().name());
        details.put("executedLate", late);
        if (command.plannedAt() != null)
            details.put("plannedAt", command.plannedAt().toString());
        audit.write(AuditLogEntry.builder()
                .action(new AuditAction("M03_BED_STATE_CHANGED", false))
                .entity("M03.BED", bedId)
                .before(before == null ? Map.of() : Map.of("state", before.name()))
                .after(afterValues)
                .reason(command.reason())
                .basis(command.basis())
                .details(details)
                .build());
        if (late)
            events.publishEvent(new DelayedScheduledWork(
                    "BED_STATE",
                    bedId,
                    command.plannedAt(),
                    now,
                    new BedNotificationContext(
                            now, bed.floorId(), List.of(new RecipientGroup.FacilityRole(Role.NURSING_HOME_MANAGER)))));
        return change;
    }

    private static BusinessException invalid() {
        return new BusinessException(
                HttpStatus.CONFLICT, "INVALID_BED_TRANSITION", "Chuyển trạng thái giường không hợp lệ");
    }

    /** Giữ nguyên trạng thái khi người đang tạm vắng chuyển viện; chỉ đồng bộ hạn từ nguồn lưu trú. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void refreshAbsentHold(
            long bedId, BedHold hold, com.eldercare.backend.administration.auditlog.api.dto.Basis basis) {
        store.lock(bedId);
        if (hold.reason() != HoldReason.ABSENT_RESIDENT || !store.refreshAbsentHold(bedId, hold)) throw invalid();
        audit.write(AuditLogEntry.builder()
                .action(new AuditAction("M03_BED_HOLD_UPDATED", false))
                .entity("M03.BED", bedId)
                .after(
                        hold.until() == null
                                ? Map.of()
                                : Map.of("holdUntil", hold.until().toString()))
                .basis(basis)
                .build());
    }
}
