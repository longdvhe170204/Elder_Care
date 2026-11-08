package com.eldercare.backend.roombedallocation.internal.allocation.service;

import com.eldercare.backend.administration.account.api.*;
import com.eldercare.backend.administration.account.api.dto.*;
import com.eldercare.backend.administration.approval.api.ApprovalRequestApi;
import com.eldercare.backend.administration.approval.api.dto.CreateRequest;
import com.eldercare.backend.administration.auditlog.api.*;
import com.eldercare.backend.administration.auditlog.api.dto.AuditAction;
import com.eldercare.backend.administration.auditlog.api.dto.AuditLogEntry;
import com.eldercare.backend.administration.auditlog.api.dto.Basis;
import com.eldercare.backend.administration.parameter.api.ParameterApi;
import com.eldercare.backend.administration.parameter.api.dto.ParameterCode;
import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.data.BusinessCommand;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.error.PermissionDeniedException;
import com.eldercare.backend.common.error.UnmetCondition;
import com.eldercare.backend.common.notification.RecipientGroup;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.resident.api.ResidentStatusApi;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import com.eldercare.backend.roombedallocation.api.dto.BedNotificationContext;
import com.eldercare.backend.roombedallocation.api.events.BedTransferred;
import com.eldercare.backend.roombedallocation.api.events.DifferentPriceBedTransfer;
import com.eldercare.backend.roombedallocation.api.spi.AllocationInfectionStatus;
import com.eldercare.backend.roombedallocation.api.spi.AllocationResidencySource;
import com.eldercare.backend.roombedallocation.internal.allocation.domain.AllocationStatus;
import com.eldercare.backend.roombedallocation.internal.allocation.domain.BedTransferOutcome;
import com.eldercare.backend.roombedallocation.internal.allocation.persistence.*;
import com.eldercare.backend.roombedallocation.internal.bed.domain.*;
import com.eldercare.backend.roombedallocation.internal.bed.service.BedStateService;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import tools.jackson.databind.json.JsonMapper;

/**
 * T032: chuyển trực tiếp tại thời điểm hiện tại. T034, T036 (FR-027): {@code transferAt} trong quá khứ là nhập bù; lùi
 * không quá CFG-M03-07 thì chuyển ngay kèm dấu nhập bù, lùi xa hơn thì lập yêu cầu {@code M03.NHAP_BU_PHAN_BO}, handler
 * gọi {@link #applyBackfill} khi yêu cầu được áp dụng.
 */
@Service
public class BedTransferService {
    private final AllocationCommandStore allocations;
    private final BedTransferStore store;
    private final AllocationCheckStore beds;
    private final AllocationLocks locks;
    private final AllocationChecker checker;
    private final ResidentAllocationFacts facts;
    private final BedStateService states;
    private final CurrentUser user;
    private final PermissionApi permissions;
    private final BusinessTime time;
    private final AuditLogApi audit;
    private final ApplicationEventPublisher events;
    private final ObjectProvider<AllocationResidencySource> residency;
    private final ObjectProvider<AllocationInfectionStatus> infection;
    private final ObjectProvider<ApprovalRequestApi> approvals;
    private final ParameterApi parameters;
    private final ResidentStatusApi statuses;
    private final JsonMapper json;

    public BedTransferService(
            AllocationCommandStore allocations,
            BedTransferStore store,
            AllocationCheckStore beds,
            AllocationLocks locks,
            AllocationChecker checker,
            ResidentAllocationFacts facts,
            BedStateService states,
            CurrentUser user,
            PermissionApi permissions,
            BusinessTime time,
            AuditLogApi audit,
            ApplicationEventPublisher events,
            ObjectProvider<AllocationResidencySource> residency,
            ObjectProvider<AllocationInfectionStatus> infection,
            ObjectProvider<ApprovalRequestApi> approvals,
            ParameterApi parameters,
            ResidentStatusApi statuses,
            JsonMapper json) {
        this.allocations = allocations;
        this.store = store;
        this.beds = beds;
        this.locks = locks;
        this.checker = checker;
        this.facts = facts;
        this.states = states;
        this.user = user;
        this.permissions = permissions;
        this.time = time;
        this.audit = audit;
        this.events = events;
        this.residency = residency;
        this.infection = infection;
        this.approvals = approvals;
        this.parameters = parameters;
        this.statuses = statuses;
        this.json = json;
    }

    @RequiresPermission(function = Function.BED_ALLOCATION, operation = Operation.CREATE)
    @BusinessCommand
    @org.springframework.resilience.annotation.Retryable(
            includes = org.springframework.dao.PessimisticLockingFailureException.class,
            maxRetries = 2,
            delay = 50)
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public long transfer(long id, long target, long reasonId, Reason reason) {
        return move(id, target, reasonId, reason, null, user.accountId(), true);
    }

    /** T036: {@code transferAt} null hoặc không ở quá khứ là chuyển bây giờ; ở quá khứ là nhập bù (FR-027). */
    @RequiresPermission(function = Function.BED_ALLOCATION, operation = Operation.CREATE)
    @BusinessCommand
    @org.springframework.resilience.annotation.Retryable(
            includes = org.springframework.dao.PessimisticLockingFailureException.class,
            maxRetries = 2,
            delay = 50)
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public BedTransferOutcome transfer(long id, long target, long reasonId, Reason reason, Instant transferAt) {
        var now = time.now().truncatedTo(ChronoUnit.MICROS);
        if (transferAt == null || !transferAt.isBefore(now))
            return new BedTransferOutcome.Moved(move(id, target, reasonId, reason, null, user.accountId(), true));
        var at = transferAt.truncatedTo(ChronoUnit.MICROS);
        Duration limit = parameters.read(ParameterCode.CFG_M03_07).asDuration();
        if (!Duration.between(at, now).minus(limit).isPositive())
            return new BedTransferOutcome.Moved(move(id, target, reasonId, reason, at, user.accountId(), true));
        return requestBackfill(id, target, reasonId, reason, at);
    }

    /**
     * FR-027 (b): áp dụng yêu cầu nhập bù đã duyệt trong transaction áp dụng của 000; người thực hiện là người lập yêu cầu,
     * không kiểm quyền tạo của người duyệt. Điều kiện không còn thỏa thì ném lỗi, cả lệnh rollback.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public long applyBackfill(long id, long target, long reasonId, Reason reason, Instant at, long requester) {
        return move(id, target, reasonId, reason, at, requester, false);
    }

    /** Điều kiện kiểm lại khi áp dụng yêu cầu nhập bù (FR-027 (c), (d)); rỗng là thỏa. */
    @Transactional(readOnly = true)
    public List<UnmetCondition> backfillConditions(long id, long target, Instant at) {
        var old = allocations.get(id);
        if (old.status() != AllocationStatus.ACTIVE || old.bedId() == target || !at.isAfter(old.startAt()))
            return List.of(new UnmetCondition(
                    "ALLOCATION_NOT_TRANSFERABLE",
                    "Phân bổ không còn Hiệu lực hoặc thời điểm chuyển trước khi phân bổ bắt đầu",
                    "FR-027(c)",
                    null,
                    null));
        var errors = new ArrayList<>(checker.check(old.residentId(), target, at, old.expectedEnd(), id, false));
        errors.addAll(absenceOverlap(old.residentId(), at));
        return List.copyOf(errors);
    }

    private BedTransferOutcome requestBackfill(long id, long target, long reasonId, Reason reason, Instant at) {
        var snapshot = allocations.get(id);
        locks.lock(snapshot.residentId(), List.of(snapshot.bedId(), target));
        var old = allocations.current(id);
        var from = beds.currentCandidate(old.bedId());
        var to = beds.currentCandidate(target);
        scope(from.bedId(), from.floorId());
        scope(to.bedId(), to.floorId());
        store.medicalSafety(reasonId);
        var unmet = backfillConditions(id, target, at);
        if (!unmet.isEmpty()) throw unmet(unmet);
        var api = approvals.getIfAvailable();
        if (api == null)
            throw new BusinessException(
                    HttpStatus.SERVICE_UNAVAILABLE, "SOURCE_UNAVAILABLE", "Chưa có nguồn yêu cầu phê duyệt");
        var content = json.createObjectNode()
                .put("currentAllocationId", id)
                .put("newBedId", target)
                .put("transferReasonId", reasonId)
                .put("transferAt", at.toString());
        var request = api.create(new CreateRequest(BackfillRequestHandler.TYPE, content, time.today(), reason, true));
        store.backfillRequested(request.id(), old.residentId(), id, target, at);
        return new BedTransferOutcome.BackfillRequested(request.id(), request.status());
    }

    /** {@code backfillAt} null là chuyển tại bây giờ; khác null là nhập bù tại thời điểm đó (FR-027 (e)). */
    private long move(
            long id, long target, long reasonId, Reason reason, Instant backfillAt, long actor, boolean checkScope) {
        var snapshot = allocations.get(id);
        locks.lock(snapshot.residentId(), List.of(snapshot.bedId(), target));
        var old = allocations.current(id);
        var from = beds.currentCandidate(old.bedId());
        var to = beds.currentCandidate(target);
        if (checkScope) {
            scope(from.bedId(), from.floorId());
            scope(to.bedId(), to.floorId());
        }
        if (old.status() != AllocationStatus.ACTIVE || old.bedId() == target)
            throw conflict("ALLOCATION_NOT_TRANSFERABLE", "Cần phân bổ đang Hiệu lực và giường đích khác");
        // Chủ dự án giữ SPI hiện hành: chưa suy giá hợp đồng để giải quyết dấu chờ qua lần chuyển tiếp.
        if (store.awaitingPriceResolution(id))
            throw new BusinessException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "SOURCE_UNAVAILABLE",
                    "Chưa có nguồn đối chiếu giá hợp đồng cho phân bổ đang chờ xử lý khác giá");
        var now = time.now().truncatedTo(ChronoUnit.MICROS);
        var at = backfillAt == null ? now : backfillAt;
        if (!at.isAfter(old.startAt())
                || old.expectedEnd() != null && !old.expectedEnd().isAfter(at))
            throw conflict("ALLOCATION_TIME_OVERLAP", "Khoảng chuyển không hợp lệ");
        var source = residency.getIfAvailable();
        var infections = infection.getIfAvailable();
        if (source == null || infections == null)
            throw new BusinessException(
                    HttpStatus.SERVICE_UNAVAILABLE, "SOURCE_UNAVAILABLE", "Chưa đủ nguồn kiểm chuyển giường");
        var cordon = infections.cordonedAt(at);
        if (from.isolated()
                || cordon.floorIds().contains(from.floorId())
                || cordon.areaIds().contains(from.areaId()))
            throw conflict("INFECTION_CONTROL_TRANSFER_REQUIRED", "Cần lệnh chuyển theo chỉ định kiểm soát lây nhiễm");
        var violations =
                new ArrayList<>(checker.checkCurrent(old.residentId(), target, at, old.expectedEnd(), id, false));
        if (backfillAt != null) violations.addAll(absenceOverlap(old.residentId(), at));
        if (!violations.isEmpty()) throw unmet(violations);
        boolean medical = store.medicalSafety(reasonId);
        boolean different = source.differentUnitPrice(
                old.residentId(), store.roomType(old.bedId()), store.roomType(target), time.today());
        if (different && !medical)
            throw conflict(
                    "RESIDENCY_CHANGE_REQUEST_REQUIRED", "Cần yêu cầu thay đổi lưu trú đã duyệt khi chuyển khác giá");
        var hold = store.absentHold(old.bedId());
        allocations.close(id, at, "BED_TRANSFER", actor);
        long next = allocations.create(
                old.residentId(),
                target,
                AllocationStatus.ACTIVE,
                at,
                old.expectedEnd(),
                reason.value(),
                actor,
                facts.careLevel(old.residentId()).orElseThrow());
        if (backfillAt != null) allocations.backfilled(next, now);
        store.transferred(id, next, reasonId, different, at);
        states.transition(old.bedId(), transition(BedEvent.TRANSFERRED_OUT, id, reason, null, id, actor));
        states.transition(target, transition(BedEvent.ALLOCATION_STARTED, next, reason, null, null, actor));
        if (hold != null)
            states.transition(target, transition(BedEvent.ABSENT_KEEP_BED, next, reason, hold, null, actor));
        source.allocated(old.residentId(), next);
        audit.write(AuditLogEntry.builder()
                .action(new AuditAction("M03_BED_TRANSFER", true))
                .entity("M03.BED_ALLOCATION", next)
                .resident(old.residentId())
                .before(Map.of("allocationId", id, "bedId", old.bedId(), "floorId", from.floorId()))
                .after(Map.of(
                        "allocationId",
                        next,
                        "bedId",
                        target,
                        "floorId",
                        to.floorId(),
                        "transferredAt",
                        at.toString(),
                        "transferReasonId",
                        reasonId,
                        "backfilled",
                        backfillAt != null,
                        "recordedAt",
                        now.toString()))
                .reason(reason)
                .build());
        events.publishEvent(new BedTransferred(old.residentId(), id, next, from.floorId(), to.floorId(), at));
        if (different)
            events.publishEvent(new DifferentPriceBedTransfer(
                    next,
                    old.residentId(),
                    actor,
                    new BedNotificationContext(
                            at,
                            to.floorId(),
                            List.of(
                                    new RecipientGroup.FacilityRole(Role.OFFICE_STAFF),
                                    new RecipientGroup.FacilityRole(Role.NURSING_HOME_MANAGER)))));
        return next;
    }

    private List<UnmetCondition> absenceOverlap(long residentId, Instant at) {
        boolean absent = !statuses.statusPeriods(
                        residentId,
                        Set.of(
                                ResidentStatus.TEMPORARILY_ABSENT,
                                ResidentStatus.HOSPITALIZED,
                                ResidentStatus.OFF_SITE_ACTIVITY),
                        at,
                        time.now())
                .isEmpty();
        return absent
                ? List.of(new UnmetCondition(
                        "BACKFILL_ABSENCE_OVERLAP",
                        "Khoảng nhập bù chồng lên khoảng Tạm vắng, Điều trị tại bệnh viện hoặc Hoạt động bên ngoài",
                        "FR-027(c)",
                        null,
                        null))
                : List.of();
    }

    private void scope(long bed, long floor) {
        if (!permissions
                .hasPermission(
                        user,
                        new PermissionKey(Function.BED_ALLOCATION, Operation.CREATE),
                        new Target("M03.BED", Long.toString(bed), null, floor))
                .allowed()) throw new PermissionDeniedException("Giường ngoài phạm vi chuyển");
    }

    private BedTransition transition(BedEvent event, long id, Reason reason, BedHold hold, Long closed, long actor) {
        return new BedTransition(
                event,
                reason,
                new Basis("FR-028", null, id, null),
                "BED_ALLOCATION",
                id,
                null,
                actor,
                hold,
                closed,
                null,
                null,
                null,
                false,
                false);
    }

    private static BusinessException unmet(List<UnmetCondition> violations) {
        return new BusinessException(
                HttpStatus.CONFLICT,
                "UNMET_CONDITIONS",
                "Điều kiện giường đích chưa thỏa",
                Map.of("unmetConditions", violations));
    }

    private BusinessException conflict(String code, String message) {
        return new BusinessException(HttpStatus.CONFLICT, code, message);
    }
}
