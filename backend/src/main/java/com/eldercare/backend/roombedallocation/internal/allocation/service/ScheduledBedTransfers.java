package com.eldercare.backend.roombedallocation.internal.allocation.service;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.administration.auditlog.api.dto.*;
import com.eldercare.backend.administration.parameter.api.ParameterApi;
import com.eldercare.backend.administration.parameter.api.dto.ParameterCode;
import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.error.*;
import com.eldercare.backend.common.notification.*;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.roombedallocation.api.dto.*;
import com.eldercare.backend.roombedallocation.api.events.*;
import com.eldercare.backend.roombedallocation.api.spi.AllocationInfectionStatus;
import com.eldercare.backend.roombedallocation.internal.allocation.domain.*;
import com.eldercare.backend.roombedallocation.internal.allocation.persistence.*;
import com.eldercare.backend.roombedallocation.internal.bed.domain.*;
import com.eldercare.backend.roombedallocation.internal.bed.service.BedStateService;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

/** T033: lệnh nội bộ MANDATORY, quyền duyệt/áp dụng thuộc lệnh nguồn 004/000. */
@Service
@Transactional(propagation = Propagation.MANDATORY)
public class ScheduledBedTransfers {
    private final ScheduledTransferStore store;
    private final AllocationCommandStore allocations;
    private final AllocationCheckStore beds;
    private final BedTransferStore transfers;
    private final AllocationLocks locks;
    private final AllocationChecker checker;
    private final ResidentAllocationFacts facts;
    private final BedStateService states;
    private final CurrentUser user;
    private final BusinessTime time;
    private final ParameterApi parameters;
    private final AuditLogApi audit;
    private final ApplicationEventPublisher events;
    private final ObjectProvider<AllocationInfectionStatus> infection;

    public ScheduledBedTransfers(
            ScheduledTransferStore store,
            AllocationCommandStore allocations,
            AllocationCheckStore beds,
            BedTransferStore transfers,
            AllocationLocks locks,
            AllocationChecker checker,
            ResidentAllocationFacts facts,
            BedStateService states,
            CurrentUser user,
            BusinessTime time,
            ParameterApi parameters,
            AuditLogApi audit,
            ApplicationEventPublisher events,
            ObjectProvider<AllocationInfectionStatus> infection) {
        this.store = store;
        this.allocations = allocations;
        this.beds = beds;
        this.transfers = transfers;
        this.locks = locks;
        this.checker = checker;
        this.facts = facts;
        this.states = states;
        this.user = user;
        this.time = time;
        this.parameters = parameters;
        this.audit = audit;
        this.events = events;
        this.infection = infection;
    }

    public List<UnmetCondition> checkReservation(long residentId, long target, LocalDate date) {
        locks.lock(residentId, List.of());
        var old = store.active(residentId);
        if (old.isEmpty())
            return invalid("ACTIVE_ALLOCATION_REQUIRED", "Cần phân bổ đang Hiệu lực để chuyển theo lịch");
        locks.lock(residentId, List.of(old.get().bedId(), target));
        if (store.hasFuture(residentId)) return invalid("ALREADY_ALLOCATED", "Đã có phân bổ Tương lai");
        var start = date.atStartOfDay(BusinessTime.ZONE).toInstant();
        if (date.isBefore(time.today())
                || old.get().expectedEnd() != null && !old.get().expectedEnd().isAfter(start))
            return invalid("ALLOCATION_TIME_OVERLAP", "Ngày chuyển ngoài khoảng lưu trú");
        if (old.get().bedId() == target)
            return invalid("ALLOCATION_NOT_TRANSFERABLE", "Giường đích phải khác giường hiện tại");
        var conditions = new ArrayList<>(
                checker.checkCurrent(residentId, target, start, old.get().expectedEnd(), null, true));
        conditions.addAll(sourceIsolation(old.get().bedId(), time.now()));
        return List.copyOf(conditions);
    }

    public long reserve(long requestId, long residentId, long target, LocalDate date) {
        locks.lock(residentId, List.of());
        var existing = store.find(requestId, true);
        if (existing.isPresent()) {
            var row = existing.get().allocation();
            if (row.residentId() != residentId
                    || row.bedId() != target
                    || !existing.get()
                            .plannedStart()
                            .equals(date.atStartOfDay(BusinessTime.ZONE).toInstant())
                    || row.status() == AllocationStatus.CANCELLED)
                throw conflict("SCHEDULED_ALLOCATION_MISMATCH", "Căn cứ yêu cầu đã gắn phân bổ khác");
            return row.id();
        }
        var conditions = checkReservation(residentId, target, date);
        if (!conditions.isEmpty()) throw new ConditionNotMetException(conditions);
        var old = store.active(residentId).orElseThrow();
        var planned = date.atStartOfDay(BusinessTime.ZONE).toInstant();
        long next = allocations.create(
                residentId,
                target,
                AllocationStatus.FUTURE,
                planned,
                old.expectedEnd(),
                "Chuyển theo yêu cầu đã duyệt",
                user.accountId(),
                facts.careLevel(residentId).orElseThrow());
        store.reserved(next, requestId, old.id(), planned);
        log("M03_SCHEDULED_TRANSFER_RESERVED", next, residentId, requestId, Map.of("bedId", target));
        return next;
    }

    public List<UnmetCondition> checkTransfer(long requestId) {
        var scheduled = lock(requestId);
        if (scheduled.allocation().status() == AllocationStatus.ACTIVE
                || scheduled.allocation().status() == AllocationStatus.CLOSED) return List.of();
        if (scheduled.allocation().status() != AllocationStatus.FUTURE)
            return invalid("FUTURE_ALLOCATION_REQUIRED", "Phân bổ theo lịch đã bị hủy");
        return transferConditions(scheduled);
    }

    public ScheduledTransferResult apply(long requestId, Instant plannedAt) {
        var scheduled = lock(requestId);
        // Mốc 00:00 của ngày đã duyệt là nguồn chuẩn, kể cả caller truyền nhầm giờ chạy.
        plannedAt = scheduled.plannedStart();
        var next = scheduled.allocation();
        if (next.status() == AllocationStatus.ACTIVE || next.status() == AllocationStatus.CLOSED)
            return ScheduledTransferResult.TRANSFERRED;
        if (next.status() == AllocationStatus.CANCELLED) return ScheduledTransferResult.CANCELLED;
        if (time.now().isBefore(next.startAt()))
            throw conflict("SCHEDULED_TRANSFER_NOT_DUE", "Chưa tới ngày chuyển đã duyệt");
        var conditions = transferConditions(scheduled);
        if (!conditions.isEmpty()) {
            if (!scheduled.waiting()) throw new ConditionNotMetException(conditions);
            cancel(requestId, new Reason("Điều kiện chuyển không còn thỏa sau khi chờ giường (Q-322)"));
            var floor = beds.currentCandidate(next.bedId()).floorId();
            events.publishEvent(new AllocationNoLongerSuitable(
                    next.id(),
                    conditions,
                    new BedNotificationContext(
                            time.now(),
                            floor,
                            List.of(
                                    new RecipientGroup.FacilityRole(Role.OFFICE_STAFF),
                                    new RecipientGroup.FloorManager(floor, new Substitution.Default()),
                                    new RecipientGroup.FacilityRole(Role.NURSING_HOME_MANAGER)))));
            return ScheduledTransferResult.CANCELLED;
        }
        var target = beds.currentCandidate(next.bedId());
        var old = store.active(next.residentId()).orElseThrow();
        if (checker
                .checkCurrent(next.residentId(), next.bedId(), time.now(), old.expectedEnd(), next.id(), true)
                .stream()
                .anyMatch(c -> "ALLOCATION_ASSET_FLAG".equals(c.code())))
            throw new BusinessException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "SOURCE_UNAVAILABLE",
                    "Chưa có kết nối gỡ dấu tài sản019 để tiếp tục chuyển đang chờ");
        if (target.state() == BedStatus.AWAITING_READY) {
            if (store.waitForBed(next.id())) {
                log(
                        "M03_SCHEDULED_TRANSFER_WAITING",
                        next.id(),
                        next.residentId(),
                        requestId,
                        Map.of("bedId", next.bedId()));
                events.publishEvent(new ScheduledTransferWaitingForBed(
                        next.id(), next.bedId(), notification(target.floorId(), false)));
            }
            return ScheduledTransferResult.AWAITING_READY;
        }
        var from = beds.currentCandidate(old.bedId());
        var at = time.now().truncatedTo(ChronoUnit.MICROS);
        var planned = scheduled.waiting() ? null : plannedAt.truncatedTo(ChronoUnit.MICROS);
        boolean late = planned != null
                && Duration.between(planned, at)
                                .compareTo(parameters
                                        .read(ParameterCode.CFG_M03_08)
                                        .asDuration())
                        > 0;
        var hold = transfers.absentHold(old.bedId());
        store.closedMetadata(old.id(), plannedAt, late);
        allocations.close(old.id(), at, "BED_TRANSFER", user.isScheduler() ? null : user.accountId());
        store.activated(
                next.id(),
                old.id(),
                at,
                old.expectedEnd(),
                facts.careLevel(next.residentId()).orElseThrow(),
                plannedAt,
                late,
                user.isScheduler() ? null : user.accountId());
        states.transition(
                old.bedId(), transition(BedEvent.TRANSFERRED_OUT, old.id(), requestId, planned, null, old.id()));
        states.transition(
                next.bedId(), transition(BedEvent.ALLOCATION_STARTED, next.id(), requestId, planned, null, null));
        if (hold != null)
            states.transition(
                    next.bedId(), transition(BedEvent.ABSENT_KEEP_BED, next.id(), requestId, planned, hold, null));
        log(
                "M03_SCHEDULED_TRANSFER_APPLIED",
                next.id(),
                next.residentId(),
                requestId,
                Map.of(
                        "previousAllocationId",
                        old.id(),
                        "bedId",
                        next.bedId(),
                        "plannedAt",
                        plannedAt.toString(),
                        "executedLate",
                        late));
        events.publishEvent(
                new BedTransferred(next.residentId(), old.id(), next.id(), from.floorId(), target.floorId(), at));
        if (late)
            events.publishEvent(new DelayedScheduledWork(
                    "BED_TRANSFER", next.id(), plannedAt, at, notification(target.floorId(), true)));
        return ScheduledTransferResult.TRANSFERRED;
    }

    public void cancel(long requestId, Reason reason) {
        // Không có đặt trước vẫn là hủy idempotent khi nguồn004 kết thúc yêu cầu.
        if (store.find(requestId, false).isEmpty()) return;
        var scheduled = lock(requestId);
        var row = scheduled.allocation();
        if (row.status() == AllocationStatus.CANCELLED) return;
        if (row.status() != AllocationStatus.FUTURE)
            throw conflict("FUTURE_ALLOCATION_REQUIRED", "Không hủy phân bổ đã bắt đầu");
        store.stopWaiting(row.id());
        allocations.cancel(row.id(), reason.value());
        log(
                "M03_SCHEDULED_TRANSFER_CANCELLED",
                row.id(),
                row.residentId(),
                requestId,
                Map.of("reason", reason.value()));
    }

    public void requestCreated(long id, long requestId) {
        var row = lockAllocation(id);
        if (row.status() != AllocationStatus.ACTIVE) return;
        if (store.linkUrgentRequest(id, requestId))
            log("M03_URGENT_TRANSFER_REQUEST_LINKED", id, row.residentId(), requestId, Map.of());
    }

    public void requestResult(long id, boolean approved) {
        var row = lockAllocation(id);
        if (row.status() != AllocationStatus.ACTIVE) return;
        if (store.decideUrgentRequest(id, approved, time.now()))
            log("M03_URGENT_TRANSFER_REQUEST_DECIDED", id, row.residentId(), null, Map.of("approved", approved));
    }

    private ScheduledAllocation lock(long requestId) {
        var snapshot = store.find(requestId, false).orElseThrow(NotFoundException::new);
        long resident = snapshot.allocation().residentId();
        locks.lock(resident, List.of());
        var bedIds = new ArrayList<Long>();
        bedIds.add(snapshot.allocation().bedId());
        store.active(resident).ifPresent(old -> bedIds.add(old.bedId()));
        locks.lock(resident, bedIds);
        return store.find(requestId, true).orElseThrow(NotFoundException::new);
    }

    private AllocationRecord lockAllocation(long id) {
        var snapshot = allocations.get(id);
        locks.lock(snapshot.residentId(), List.of(snapshot.bedId()));
        return allocations.current(id);
    }

    private List<UnmetCondition> transferConditions(ScheduledAllocation scheduled) {
        var next = scheduled.allocation();
        var old = store.active(next.residentId());
        if (old.isEmpty() || old.get().bedId() == next.bedId())
            return invalid("ACTIVE_ALLOCATION_REQUIRED", "Không còn phân bổ nguồn để chuyển");
        var at = time.now().truncatedTo(ChronoUnit.MICROS);
        if (!at.isAfter(old.get().startAt())
                || old.get().expectedEnd() != null && !old.get().expectedEnd().isAfter(at))
            return invalid("ALLOCATION_TIME_OVERLAP", "Thời điểm chuyển ngoài khoảng lưu trú");
        var target = beds.currentCandidate(next.bedId());
        var conditions = new ArrayList<>(
                checker
                        .checkCurrent(
                                next.residentId(), next.bedId(), at, old.get().expectedEnd(), next.id(), true)
                        .stream()
                        .filter(c -> !"ALLOCATION_ASSET_FLAG".equals(c.code())
                                && !"ALLOCATION_NOT_READY_BEFORE_DEADLINE".equals(c.code()))
                        .toList());
        // Chỉ Chờ sẵn sàng/dấu tài sản được hoãn, không miễn điều kiện cơ cấu hay trạng thái khác.
        if (target.state() == BedStatus.HELD)
            conditions.addAll(invalid("ALLOCATION_BED_STATE", "Giường đích đang giữ chỗ"));
        conditions.addAll(sourceIsolation(old.get().bedId(), at));
        return List.copyOf(conditions);
    }

    private List<UnmetCondition> sourceIsolation(long bed, Instant at) {
        var source = infection.getIfAvailable();
        if (source == null)
            throw new BusinessException(
                    HttpStatus.SERVICE_UNAVAILABLE, "SOURCE_UNAVAILABLE", "Chưa có nguồn kiểm lây nhiễm");
        var row = beds.currentCandidate(bed);
        var cordon = source.cordonedAt(at);
        return row.isolated()
                        || cordon.floorIds().contains(row.floorId())
                        || cordon.areaIds().contains(row.areaId())
                ? invalid("INFECTION_CONTROL_TRANSFER_REQUIRED", "Cần chuyển theo chỉ định kiểm soát lây nhiễm")
                : List.of();
    }

    private BedTransition transition(
            BedEvent event, long id, long request, Instant planned, BedHold hold, Long closed) {
        return new BedTransition(
                event,
                null,
                new Basis("FR-029", request, id, null),
                "REQUEST",
                request,
                planned,
                user.isScheduler() ? null : user.accountId(),
                hold,
                closed,
                null,
                null,
                null,
                false,
                false);
    }

    private BedNotificationContext notification(long floor, boolean managerOnly) {
        return new BedNotificationContext(
                time.now(),
                floor,
                managerOnly
                        ? List.of(new RecipientGroup.FacilityRole(Role.NURSING_HOME_MANAGER))
                        : List.of(
                                new RecipientGroup.FacilityRole(Role.OFFICE_STAFF),
                                new RecipientGroup.FloorManager(floor, new Substitution.Default())));
    }

    private void log(String action, long id, long resident, Long request, Map<String, Object> after) {
        audit.write(AuditLogEntry.builder()
                .action(new AuditAction(action, false))
                .entity("M03.BED_ALLOCATION", id)
                .resident(resident)
                .after(after)
                .basis(new Basis("FR-029", request, id, null))
                .build());
    }

    private static List<UnmetCondition> invalid(String code, String message) {
        return List.of(new UnmetCondition(code, message));
    }

    private static BusinessException conflict(String code, String message) {
        return new BusinessException(HttpStatus.CONFLICT, code, message);
    }
}
