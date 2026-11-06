package com.eldercare.backend.roombedallocation.internal.allocation.service;

import com.eldercare.backend.administration.account.api.AccountApi;
import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.administration.auditlog.api.dto.AuditAction;
import com.eldercare.backend.administration.auditlog.api.dto.AuditLogEntry;
import com.eldercare.backend.administration.auditlog.api.dto.Basis;
import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.error.ConditionNotMetException;
import com.eldercare.backend.common.error.UnmetCondition;
import com.eldercare.backend.common.notification.*;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.roombedallocation.api.dto.BedNotificationContext;
import com.eldercare.backend.roombedallocation.api.dto.BedStatus;
import com.eldercare.backend.roombedallocation.api.dto.ScheduledTransferResult;
import com.eldercare.backend.roombedallocation.api.events.BedTransferred;
import com.eldercare.backend.roombedallocation.api.events.ScheduledTransferWaitingForBed;
import com.eldercare.backend.roombedallocation.api.spi.AllocationResidencySource;
import com.eldercare.backend.roombedallocation.internal.allocation.domain.AllocationRecord;
import com.eldercare.backend.roombedallocation.internal.allocation.domain.AllocationStatus;
import com.eldercare.backend.roombedallocation.internal.allocation.domain.ScheduledTransferNotice;
import com.eldercare.backend.roombedallocation.internal.allocation.persistence.AllocationCheckStore;
import com.eldercare.backend.roombedallocation.internal.allocation.persistence.AllocationCommandStore;
import com.eldercare.backend.roombedallocation.internal.allocation.persistence.BedTransferStore;
import com.eldercare.backend.roombedallocation.internal.bed.domain.BedAssetPort;
import com.eldercare.backend.roombedallocation.internal.bed.domain.BedEvent;
import com.eldercare.backend.roombedallocation.internal.bed.domain.BedHold;
import com.eldercare.backend.roombedallocation.internal.bed.domain.BedTransition;
import com.eldercare.backend.roombedallocation.internal.bed.service.BedStateService;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 003 T033 lượt 2 (FR-029, FR-011a, FR-030, FR-031; research G-07; data-model mục phân bổ): chuyển giường theo lịch đi
 * qua yêu cầu thay đổi lưu trú của 004. Lúc duyệt: kiểm FR-018 và đặt trước phân bổ Tương lai (căn cứ là yêu cầu). Ngày
 * hiệu lực: giường mới Trống thì chuyển ngay; chưa Trống hoặc mang dấu "chờ chuyển người" thì giữ Tương lai, đánh dấu chờ
 * giường và báo. Khi giường về Trống (Xác nhận sẵn sàng, gỡ dấu), {@link #startWaiting} chuyển, hoặc hủy và báo khi FR-018
 * không còn thỏa (chủ dự án chốt 2026-11-06: xử lý trong 003, không đổi {@link ScheduledTransferResult}). Kết quả yêu cầu
 * sau chuyển gấp: gỡ dấu chờ lập yêu cầu; từ chối thì đặt dấu chờ chuyển về phòng cùng giá; báo người thực hiện lệnh
 * chuyển và Hành chính (chủ dự án chốt 2026-11-06). Mọi hàm chạy trong transaction của lệnh gọi.
 */
@Service
@Transactional(propagation = Propagation.MANDATORY)
public class ScheduledTransfers {
    /** Mã FR-018 không tính là vi phạm của chuyển theo lịch: giường chưa Trống thì chờ (FR-011a). */
    private static final Set<String> WAITABLE =
            Set.of("ALLOCATION_BED_STATE", "ALLOCATION_ASSET_FLAG", "ALLOCATION_NOT_READY_BEFORE_DEADLINE");

    private final AllocationCommandStore allocations;
    private final AllocationCheckStore beds;
    private final BedTransferStore transfers;
    private final AllocationLocks locks;
    private final AllocationChecker checker;
    private final ResidentAllocationFacts facts;
    private final BedStateService states;
    private final CurrentUser user;
    private final AccountApi accounts;
    private final BusinessTime time;
    private final AuditLogApi audit;
    private final ApplicationEventPublisher events;
    private final ObjectProvider<AllocationResidencySource> residency;
    private final ObjectProvider<BedAssetPort> assets;

    public ScheduledTransfers(
            AllocationCommandStore allocations,
            AllocationCheckStore beds,
            BedTransferStore transfers,
            AllocationLocks locks,
            AllocationChecker checker,
            ResidentAllocationFacts facts,
            BedStateService states,
            CurrentUser user,
            AccountApi accounts,
            BusinessTime time,
            AuditLogApi audit,
            ApplicationEventPublisher events,
            ObjectProvider<AllocationResidencySource> residency,
            ObjectProvider<BedAssetPort> assets) {
        this.allocations = allocations;
        this.beds = beds;
        this.transfers = transfers;
        this.locks = locks;
        this.checker = checker;
        this.facts = facts;
        this.states = states;
        this.user = user;
        this.accounts = accounts;
        this.time = time;
        this.audit = audit;
        this.events = events;
        this.residency = residency;
        this.assets = assets;
    }

    /** FR-029 lúc duyệt: FR-018 cho giường mới từ 00:00 ngày hiệu lực tới kết thúc dự kiến của phân bổ hiện tại. */
    public List<UnmetCondition> checkReservation(long residentId, long nextBedId, LocalDate effectiveDate) {
        var active = allocations.openCurrent(residentId, true, false);
        if (active.isEmpty())
            return List.of(new UnmetCondition("ALLOCATION_NOT_ACTIVE", "Người cao tuổi chưa có phân bổ đang hiệu lực"));
        var current = active.getFirst();
        var unmet = new ArrayList<UnmetCondition>();
        if (current.bedId() == nextBedId) unmet.add(new UnmetCondition("SAME_BED", "Giường mới trùng giường hiện tại"));
        if (!allocations.openCurrent(residentId, false, true).isEmpty())
            unmet.add(new UnmetCondition("FUTURE_ALLOCATION_EXISTS", "Người cao tuổi đã có phân bổ tương lai"));
        unmet.addAll(checker.check(residentId, nextBedId, start(effectiveDate), current.expectedEnd(), null, true));
        return List.copyOf(unmet);
    }

    /** FR-029 lúc duyệt (trong {@code khiDuyet} của handler 004): đặt trước phân bổ Tương lai, căn cứ là yêu cầu. */
    public long reserve(long requestId, long residentId, long nextBedId, LocalDate effectiveDate) {
        locks.lock(residentId, List.of(nextBedId));
        var unmet = checkReservation(residentId, nextBedId, effectiveDate);
        if (!unmet.isEmpty()) throw new ConditionNotMetException(unmet);
        var current = allocations.openCurrent(residentId, true, false).getFirst();
        long id = allocations.createScheduled(
                residentId,
                nextBedId,
                start(effectiveDate),
                current.expectedEnd(),
                requestId,
                current.id(),
                actor(),
                facts.careLevel(residentId).orElseThrow());
        record(id, residentId, "M03_SCHEDULED_TRANSFER_RESERVED", "FUTURE", requestId, null);
        return id;
    }

    /** Điều kiện áp dụng (trong {@code kiemTraDieuKienApDung} của 004); giường chưa Trống không phải vi phạm. */
    public List<UnmetCondition> checkTransfer(long requestId) {
        var future = allocations.scheduledFor(requestId).filter(a -> a.status() == AllocationStatus.FUTURE);
        if (future.isEmpty())
            return List.of(
                    new UnmetCondition("SCHEDULED_ALLOCATION_MISSING", "Không có phân bổ đặt trước của yêu cầu"));
        var old = allocations.get(allocations.previousOf(future.get().id()));
        return checker
                .check(
                        future.get().residentId(),
                        future.get().bedId(),
                        now(),
                        old.expectedEnd(),
                        future.get().id(),
                        true)
                .stream()
                .filter(c -> !WAITABLE.contains(c.code()))
                .toList();
    }

    /** Ngày hiệu lực (job áp dụng của 000 qua 004): chuyển ngay, hoặc chờ giường về Trống (FR-011a). */
    public ScheduledTransferResult apply(long requestId, Instant plannedAt) {
        var future = allocations.scheduledFor(requestId).orElseThrow(() -> conflict("Không có phân bổ đặt trước"));
        if (future.status() == AllocationStatus.ACTIVE) return ScheduledTransferResult.TRANSFERRED;
        if (future.status() != AllocationStatus.FUTURE) throw conflict("Phân bổ đặt trước không còn Tương lai");
        var old = allocations.get(allocations.previousOf(future.id()));
        locks.lock(future.residentId(), List.of(old.bedId(), future.bedId()));
        var bed = beds.currentCandidate(future.bedId());
        var asset = assets.getIfAvailable();
        boolean flagged = asset != null && asset.awaitingResidentTransfer(future.bedId());
        if (bed.state() != BedStatus.EMPTY || flagged) {
            allocations.waitForBed(future.id(), plannedAt);
            record(future.id(), future.residentId(), "M03_SCHEDULED_TRANSFER_WAITING", "FUTURE", requestId, null);
            events.publishEvent(new ScheduledTransferWaitingForBed(
                    future.id(),
                    future.bedId(),
                    new BedNotificationContext(
                            now(),
                            bed.floorId(),
                            List.of(
                                    new RecipientGroup.FloorManager(bed.floorId(), new Substitution.Default()),
                                    new RecipientGroup.FacilityRole(Role.OFFICE_STAFF)))));
            return ScheduledTransferResult.AWAITING_READY;
        }
        transfer(future, old, requestId, plannedAt);
        return ScheduledTransferResult.TRANSFERRED;
    }

    /**
     * FR-011a, FR-029: giường của chuyển theo lịch đang chờ vừa về Trống (trong lệnh Xác nhận sẵn sàng, gỡ dấu của 019).
     * FR-018 còn thỏa thì chuyển, trả {@code true}; không thì hủy phân bổ Tương lai, người ở lại giường cũ, báo Hành chính,
     * Trưởng tầng, Quản lý viện; yêu cầu đã áp dụng giữ nguyên; trả {@code false}.
     */
    public boolean startWaiting(long requestId, Instant plannedAt) {
        var future = allocations
                .scheduledFor(requestId)
                .filter(a -> a.status() == AllocationStatus.FUTURE)
                .orElseThrow(() -> conflict("Không có phân bổ chờ giường của yêu cầu"));
        var old = allocations.get(allocations.previousOf(future.id()));
        locks.lock(future.residentId(), List.of(old.bedId(), future.bedId()));
        var unmet =
                checker.checkCurrent(future.residentId(), future.bedId(), now(), old.expectedEnd(), future.id(), false);
        if (unmet.isEmpty()) {
            transfer(future, old, requestId, plannedAt);
            return true;
        }
        allocations.cancel(future.id(), "Không còn đạt điều kiện phân bổ khi giường sẵn sàng");
        record(future.id(), future.residentId(), "M03_SCHEDULED_TRANSFER_FAILED", "CANCELLED", requestId, null);
        long floor = beds.currentCandidate(future.bedId()).floorId();
        String title = "Chuyển giường theo lịch không thực hiện được: giường mới không còn đạt điều kiện";
        events.publishEvent(new ScheduledTransferNotice(
                future.id(),
                List.of(request(
                        "SCHEDULED_TRANSFER_FAILED",
                        future.id(),
                        future.residentId(),
                        floor,
                        title,
                        List.of(
                                new RecipientGroup.FacilityRole(Role.OFFICE_STAFF),
                                new RecipientGroup.FloorManager(floor, new Substitution.Default()),
                                new RecipientGroup.FacilityRole(Role.NURSING_HOME_MANAGER))))));
        return false;
    }

    /** Yêu cầu Áp dụng không thành (004 nghe sự kiện của 000): hủy phân bổ Tương lai; idempotent. */
    public void cancel(long requestId, Reason reason) {
        var future = allocations.scheduledFor(requestId).filter(a -> a.status() == AllocationStatus.FUTURE);
        if (future.isEmpty()) return;
        allocations.cancel(future.get().id(), reason.value());
        record(
                future.get().id(),
                future.get().residentId(),
                "M03_SCHEDULED_TRANSFER_CANCELLED",
                "CANCELLED",
                requestId,
                reason);
    }

    /** FR-030: Hành chính đã lập yêu cầu sau chuyển gấp. */
    public void requestCreated(long allocationId, long requestId) {
        var allocation = allocations.current(allocationId);
        allocations.requestCreated(allocationId);
        record(
                allocationId,
                allocation.residentId(),
                "M03_URGENT_TRANSFER_REQUEST_CREATED",
                allocation.status().name(),
                requestId,
                null);
    }

    /** FR-030: kết quả yêu cầu sau chuyển gấp; từ chối thì đặt dấu chờ chuyển về phòng cùng giá. */
    public void requestResult(long allocationId, boolean approved) {
        var allocation = allocations.current(allocationId);
        var now = now();
        if (!approved) allocations.awaitingSamePrice(allocationId, now);
        record(
                allocationId,
                allocation.residentId(),
                approved ? "M03_URGENT_TRANSFER_APPROVED" : "M03_URGENT_TRANSFER_REJECTED",
                allocation.status().name(),
                null,
                null);
        long floor = beds.currentCandidate(allocation.bedId()).floorId();
        var recipients = new ArrayList<RecipientGroup>(List.of(new RecipientGroup.FacilityRole(Role.OFFICE_STAFF)));
        allocations
                .openedBy(allocationId)
                .flatMap(accounts::byId)
                .map(a -> a.staffId())
                .ifPresent(staff -> recipients.add(new RecipientGroup.Staff(staff, "BED_TRANSFER_EXECUTOR")));
        String title = approved
                ? "Yêu cầu thay đổi lưu trú sau chuyển giường gấp đã được duyệt"
                : "Yêu cầu thay đổi lưu trú sau chuyển giường gấp bị từ chối: cần chuyển về phòng cùng giá";
        events.publishEvent(new ScheduledTransferNotice(
                allocationId,
                List.of(request(
                        "URGENT_TRANSFER_REQUEST_RESULT",
                        allocationId,
                        allocation.residentId(),
                        floor,
                        title,
                        recipients))));
    }

    // ---- Dùng chung ------------------------------------------------------------------------------------------------

    /** Đóng phân bổ cũ, bắt đầu phân bổ Tương lai tại thời điểm thực tế; người thực hiện là Bộ lập lịch khi job chạy. */
    private void transfer(AllocationRecord future, AllocationRecord previous, long requestId, Instant plannedAt) {
        var old = allocations.current(previous.id());
        if (old.status() != AllocationStatus.ACTIVE) throw conflict("Phân bổ hiện tại không còn Hiệu lực");
        var at = now();
        var unmet =
                checker.checkCurrent(future.residentId(), future.bedId(), at, old.expectedEnd(), future.id(), false);
        if (!unmet.isEmpty()) throw new ConditionNotMetException(unmet);
        var from = beds.currentCandidate(old.bedId());
        var to = beds.currentCandidate(future.bedId());
        BedHold hold = transfers.absentHold(old.bedId());
        Long actor = actor();
        allocations.close(old.id(), at, "SCHEDULED_TRANSFER", actor);
        allocations.activateScheduled(
                future.id(), at, plannedAt, facts.careLevel(future.residentId()).orElseThrow());
        var basis = new Basis("FR-029", requestId, null, null);
        states.transition(
                old.bedId(), transition(BedEvent.TRANSFERRED_OUT, basis, requestId, plannedAt, actor, null, old.id()));
        states.transition(
                future.bedId(),
                transition(BedEvent.ALLOCATION_STARTED, basis, requestId, plannedAt, actor, null, null));
        if (hold != null)
            states.transition(
                    future.bedId(),
                    transition(BedEvent.ABSENT_KEEP_BED, basis, requestId, plannedAt, actor, hold, null));
        var source = residency.getIfAvailable();
        if (source != null) source.allocated(future.residentId(), future.id());
        audit.write(AuditLogEntry.builder()
                .action(new AuditAction("M03_SCHEDULED_TRANSFER", false))
                .entity("M03.BED_ALLOCATION", future.id())
                .resident(future.residentId())
                .before(Map.of("allocationId", old.id(), "bedId", old.bedId(), "floorId", from.floorId()))
                .after(Map.of(
                        "allocationId", future.id(),
                        "bedId", future.bedId(),
                        "floorId", to.floorId(),
                        "transferredAt", at.toString(),
                        "plannedAt", plannedAt.toString()))
                .basis(basis)
                .build());
        events.publishEvent(
                new BedTransferred(future.residentId(), old.id(), future.id(), from.floorId(), to.floorId(), at));
    }

    private static BedTransition transition(
            BedEvent event, Basis basis, long requestId, Instant plannedAt, Long actor, BedHold hold, Long closed) {
        return new BedTransition(
                event,
                null,
                basis,
                "APPROVAL_REQUEST",
                requestId,
                plannedAt,
                actor,
                hold,
                closed,
                null,
                null,
                null,
                false,
                false);
    }

    private NotificationRequest request(
            String event, long id, long residentId, long floor, String title, List<RecipientGroup> to) {
        var now = now();
        return new NotificationRequest(
                "M03",
                event,
                SourceGroup.OTHER,
                new SourceRecord("M03.BED_ALLOCATION", Long.toString(id)),
                event + ":" + id + ":" + now,
                Optional.of(NotificationLevel.MEDIUM),
                Optional.of(residentId),
                Optional.of(floor),
                to,
                List.of(new ContentPart(InformationType.GENERAL, title)),
                Optional.of(title),
                Optional.of(title),
                Optional.empty(),
                false,
                now,
                Map.of("allocationId", id),
                false,
                Optional.empty(),
                Optional.empty());
    }

    private void record(long id, long residentId, String action, String status, Long requestId, Reason reason) {
        var entry = AuditLogEntry.builder()
                .action(new AuditAction(action, reason != null))
                .entity("M03.BED_ALLOCATION", id)
                .resident(residentId)
                .after(Map.of("status", status))
                .basis(new Basis("FR-029", requestId, null, null));
        if (reason != null) entry.reason(reason);
        audit.write(entry.build());
    }

    private Long actor() {
        return user.isScheduler() ? null : user.accountId();
    }

    private Instant now() {
        return time.now().truncatedTo(ChronoUnit.MICROS);
    }

    private static Instant start(LocalDate date) {
        return date.atStartOfDay(BusinessTime.ZONE).toInstant();
    }

    private static BusinessException conflict(String message) {
        return new BusinessException(HttpStatus.CONFLICT, "STATE_CHANGED", message);
    }
}
