package com.cambae.backend.m03.internal.allocation.service;

import com.cambae.backend.common.Reason;
import com.cambae.backend.common.Role;
import com.cambae.backend.common.error.*;
import com.cambae.backend.common.notification.*;
import com.cambae.backend.m01.api.dto.*;
import com.cambae.backend.m01.api.spi.StatusTransitionParticipant;
import com.cambae.backend.m03.api.dto.BedNotificationContext;
import com.cambae.backend.m03.api.dto.BedStatus;
import com.cambae.backend.m03.api.events.*;
import com.cambae.backend.m03.api.spi.*;
import com.cambae.backend.m03.internal.allocation.domain.*;
import com.cambae.backend.m03.internal.allocation.persistence.*;
import com.cambae.backend.m03.internal.bed.domain.*;
import com.cambae.backend.m03.internal.bed.service.*;
import com.cambae.backend.m15.account.api.CurrentUser;
import com.cambae.backend.m15.auditlog.api.*;
import java.time.Instant;
import java.util.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import tools.jackson.databind.JsonNode;

/** T043: tác động đồng bộ trong transaction M01; thứ tự sau nguồn lưu trú 004 (@Order 10). */
@Service
@Order(20)
public class AllocationStatusParticipant implements StatusTransitionParticipant {
    private static final String SWITCH_BED = "switchAllocationToBedId";
    private final AllocationCommandStore store;
    private final AllocationReadStore read;
    private final AllocationCheckStore checks;
    private final AllocationLocks locks;
    private final AllocationChecker checker;
    private final ResidentAllocationFacts facts;
    private final BedStateService state;
    private final BedHoldService holds;
    private final ObjectProvider<AllocationResidencySource> residency;
    private final ObjectProvider<AllocationInfectionStatus> infections;
    private final CurrentUser user;
    private final AuditLogApi audit;
    private final ApplicationEventPublisher events;
    private final WaitingHoldIndexStore holdIndex;

    public AllocationStatusParticipant(
            AllocationCommandStore store,
            AllocationReadStore read,
            AllocationCheckStore checks,
            AllocationLocks locks,
            AllocationChecker checker,
            ResidentAllocationFacts facts,
            BedStateService state,
            BedHoldService holds,
            ObjectProvider<AllocationResidencySource> residency,
            ObjectProvider<AllocationInfectionStatus> infections,
            CurrentUser user,
            AuditLogApi audit,
            ApplicationEventPublisher events,
            WaitingHoldIndexStore holdIndex) {
        this.store = store;
        this.read = read;
        this.checks = checks;
        this.locks = locks;
        this.checker = checker;
        this.facts = facts;
        this.state = state;
        this.holds = holds;
        this.residency = residency;
        this.infections = infections;
        this.user = user;
        this.audit = audit;
        this.events = events;
        this.holdIndex = holdIndex;
    }

    @Override
    public Set<StatusCommand> interestedCommands() {
        return Set.of(
                StatusCommand.COMPLETE_ADMISSION,
                StatusCommand.CANCEL_ADMISSION,
                StatusCommand.GRANT_TEMPORARY_ABSENCE,
                StatusCommand.TRANSFER_OUT,
                StatusCommand.RECORD_RETURN,
                StatusCommand.END_RESIDENCY,
                StatusCommand.RECORD_DEATH);
    }

    @Override
    public void validateDetails(StatusCommand command, JsonNode details) {
        if (command != StatusCommand.COMPLETE_ADMISSION
                || details == null
                || !details.has(SWITCH_BED)
                || details.get(SWITCH_BED).isNull()) return;
        var bed = details.get(SWITCH_BED);
        if (!bed.isIntegralNumber() || !bed.canConvertToLong() || bed.longValue() <= 0)
            throw new BusinessException(
                    HttpStatus.UNPROCESSABLE_CONTENT, "INVALID_BED", "Giường thay thế không hợp lệ");
    }

    @Override
    @Transactional(readOnly = true)
    public List<Condition> checkConditions(CommandContext context) {
        if (context.command() != StatusCommand.COMPLETE_ADMISSION) return List.of();
        var source = residency.getIfAvailable();
        if (source == null)
            return List.of(new Condition(
                    "ALLOCATION_SOURCE_UNAVAILABLE", "Chưa có nguồn kiểm loại lưu trú", "FR-018(b)", false));
        var type = source.residencyType(context.residentId());
        if (type.isPresent() && type.get() == ResidencyType.DAY_CARE) return List.of();
        if (type.isEmpty())
            return List.of(
                    new Condition("ALLOCATION_RESIDENCY_TYPE", "Chưa xác định được loại lưu trú", "FR-018(b)", false));
        var future = read.list(context.residentId(), null, AllocationStatus.FUTURE).stream()
                .findFirst();
        if (future.isEmpty()) return List.of(notReady());
        long bedId =
                replacement(context.details()).orElse(future.get().location().bedId());
        return admissionConditions(
                        context.residentId(),
                        bedId,
                        context.at(),
                        future.get().expectedEnd(),
                        future.get().id())
                .stream()
                .map(condition -> new Condition(condition.code(), condition.description(), condition.basis(), false))
                .toList();
    }

    private List<UnmetCondition> admissionConditions(
            long residentId, long bedId, Instant at, Instant end, long futureId) {
        return admissionConditions(residentId, bedId, at, end, futureId, false);
    }

    private List<UnmetCondition> admissionConditions(
            long residentId, long bedId, Instant at, Instant end, long futureId, boolean current) {
        var failures = new ArrayList<>(
                current
                        ? checker.checkCurrent(residentId, bedId, at, end, futureId, false)
                        : checker.check(residentId, bedId, at, end, futureId, false));
        var bed = current ? checks.currentCandidate(bedId) : checks.candidate(bedId);
        if (bed.state() != BedStatus.EMPTY
                && !(bed.state() == BedStatus.HELD && Objects.equals(bed.heldForResidentId(), residentId)))
            failures.add(new UnmetCondition(
                    "RESERVED_BED_NOT_READY", "Giường đặt trước chưa sẵn sàng", "FR-023", null, null));
        return List.copyOf(failures);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void applyEffects(TransitionResult result) {
        locks.lock(result.residentId(), List.of());
        boolean admission = result.command() == StatusCommand.COMPLETE_ADMISSION
                || result.command() == StatusCommand.CANCEL_ADMISSION;
        var rows = store.openCurrent(result.residentId(), !admission, true);
        var bedIds = new ArrayList<>(rows.stream().map(AllocationRecord::bedId).toList());
        bedIds.addAll(holdIndex.current(result.residentId()));
        replacement(result.details()).ifPresent(bedIds::add);
        locks.lock(result.residentId(), bedIds);
        var active = rows.stream()
                .filter(row -> row.status() == AllocationStatus.ACTIVE)
                .findFirst()
                .orElse(null);
        switch (result.command()) {
            case COMPLETE_ADMISSION -> complete(result, rows);
            case CANCEL_ADMISSION -> {
                cancelFutures(rows, "Hủy tiếp nhận", result);
                cancelHolds(result.residentId(), "Hủy tiếp nhận");
            }
            case GRANT_TEMPORARY_ABSENCE, TRANSFER_OUT -> {
                if (active == null) return;
                var source = source();
                var hold = source.holdBedWhenAbsent(result.residentId());
                boolean alreadyAbsent = result.from() == ResidentStatus.TEMPORARILY_ABSENT
                        || result.from() == ResidentStatus.HOSPITALIZED;
                if (alreadyAbsent && hold.held()) {
                    state.refreshAbsentHold(
                            active.bedId(),
                            new BedHold(
                                    HoldReason.ABSENT_RESIDENT,
                                    result.residentId(),
                                    null,
                                    hold.until().orElse(null)),
                            basis(result));
                    return;
                }
                if (hold.held())
                    change(
                            active.bedId(),
                            BedEvent.ABSENT_KEEP_BED,
                            result,
                            new BedHold(
                                    HoldReason.ABSENT_RESIDENT,
                                    result.residentId(),
                                    null,
                                    hold.until().orElse(null)),
                            null);
                else close(active, result, alreadyAbsent ? BedEvent.RELEASE_WHEN_ABSENT : BedEvent.ABSENT_RELEASE_BED);
            }
            case RECORD_RETURN -> returned(result, active);
            case END_RESIDENCY, RECORD_DEATH -> {
                if (active != null)
                    close(
                            active,
                            result,
                            result.command() == StatusCommand.RECORD_DEATH ? BedEvent.DEATH : BedEvent.RESIDENCY_ENDED);
                cancelFutures(rows, "Kết thúc hồ sơ", result);
                cancelHolds(result.residentId(), "Kết thúc hồ sơ");
                store.clearWithoutBed(result.residentId());
            }
            default -> throw new IllegalArgumentException("Lệnh ngoài phạm vi tham gia M03");
        }
    }

    private void complete(TransitionResult result, List<AllocationRecord> rows) {
        if (source().residencyType(result.residentId())
                .filter(type -> type == ResidencyType.DAY_CARE)
                .isPresent()) return;
        var future = rows.stream()
                .filter(row -> row.status() == AllocationStatus.FUTURE)
                .findFirst()
                .orElseThrow(() -> new ConditionNotMetException(
                        List.of(new UnmetCondition("RESERVED_BED_NOT_READY", "Chưa có giường đặt trước"))));
        long bedId = replacement(result.details()).orElse(future.bedId());
        var failures =
                admissionConditions(result.residentId(), bedId, result.at(), future.expectedEnd(), future.id(), true);
        if (!failures.isEmpty()) throw new ConditionNotMetException(failures);
        var level = facts.careLevel(result.residentId()).orElseThrow();
        long id = future.id();
        if (bedId != future.bedId()) {
            cancelFutures(List.of(future), "Đổi giường khi tiếp nhận", result);
            id = store.create(
                    result.residentId(),
                    bedId,
                    AllocationStatus.ACTIVE,
                    result.at(),
                    future.expectedEnd(),
                    "Hoàn tất tiếp nhận",
                    user.accountId(),
                    level);
        } else store.activate(id, result.at(), level);
        holds.cancelResidentHolds(result.residentId(), Reason.of("Hoàn tất tiếp nhận"), bedId);
        store.clearWithoutBed(result.residentId());
        change(bedId, BedEvent.ALLOCATION_STARTED, result, null, null);
        audit.write(AuditLogEntry.builder()
                .action(new AuditAction("M03_ALLOCATION_STARTED", false))
                .entity("M03.BED_ALLOCATION", id)
                .after(Map.of("status", "ACTIVE", "startAt", result.at().toString()))
                .basis(basis(result))
                .build());
        events.publishEvent(new BedAllocationStarted(id, result.residentId(), bedId, result.at()));
    }

    private void returned(TransitionResult result, AllocationRecord active) {
        if (source().residencyType(result.residentId())
                .filter(type -> type == ResidencyType.DAY_CARE)
                .isPresent()) return;
        if (active == null) {
            if (store.markWithoutBed(result.residentId(), result.at()))
                events.publishEvent(new com.cambae.backend.m03.api.events.AllocationReminder(
                        "WITHOUT_BED",
                        result.residentId(),
                        1,
                        new BedNotificationContext(
                                result.at(), null, List.of(new RecipientGroup.FacilityRole(Role.OFFICE_STAFF)))));
            return;
        }
        change(active.bedId(), BedEvent.RETURNED, result, null, null);
        var bed = checks.currentCandidate(active.bedId());
        if (blocked(bed, result.at())) {
            store.awaitingDoctor(active.id());
            events.publishEvent(new ReturnedToIsolatedRoom(
                    active.id(),
                    result.residentId(),
                    bed.roomId(),
                    new BedNotificationContext(
                            result.at(),
                            bed.floorId(),
                            List.of(
                                    new RecipientGroup.OnDutyDoctor(new Substitution.Default()),
                                    new RecipientGroup.FloorManager(bed.floorId(), new Substitution.Default())))));
        }
    }

    private void close(AllocationRecord active, TransitionResult result, BedEvent event) {
        if (!result.at().isAfter(active.startAt()))
            throw new ConditionNotMetException(
                    List.of(new UnmetCondition("ALLOCATION_TIME_OVERLAP", "Thời điểm kết thúc phải sau bắt đầu")));
        store.close(active.id(), result.at(), result.command().name(), user.isScheduler() ? null : user.accountId());
        change(active.bedId(), event, result, null, active.id());
        audit.write(AuditLogEntry.builder()
                .action(new AuditAction("M03_ALLOCATION_CLOSED", false))
                .entity("M03.BED_ALLOCATION", active.id())
                .after(Map.of("status", "CLOSED", "endAt", result.at().toString()))
                .basis(basis(result))
                .build());
        events.publishEvent(new BedAllocationClosed(
                active.id(),
                result.residentId(),
                active.bedId(),
                result.at(),
                result.command().name()));
    }

    private void cancelFutures(List<AllocationRecord> rows, String reason, TransitionResult result) {
        for (var row : rows)
            if (row.status() == AllocationStatus.FUTURE) {
                store.cancel(row.id(), reason);
                audit.write(AuditLogEntry.builder()
                        .action(new AuditAction("M03_ALLOCATION_CANCELLED", true))
                        .entity("M03.BED_ALLOCATION", row.id())
                        .before(Map.of("status", "FUTURE"))
                        .after(Map.of("status", "CANCELLED"))
                        .reason(Reason.of(reason))
                        .basis(basis(result))
                        .build());
            }
    }

    private void cancelHolds(long residentId, String reason) {
        holds.cancelResidentHolds(residentId, Reason.of(reason), null);
    }

    private void change(long bedId, BedEvent event, TransitionResult result, BedHold hold, Long closedId) {
        state.transition(
                bedId,
                new BedTransition(
                        event,
                        null,
                        basis(result),
                        "RESIDENT_STATUS",
                        result.historyId(),
                        null,
                        user.isScheduler() ? null : user.accountId(),
                        hold,
                        closedId,
                        null,
                        null,
                        null,
                        false,
                        false));
    }

    private boolean blocked(long bedId, Instant at) {
        return blocked(checks.candidate(bedId), at);
    }

    private boolean blocked(AllocationCandidate bed, Instant at) {
        if (bed.isolated()) return true;
        var source = infections.getIfAvailable();
        if (source == null) return false;
        var cordon = source.cordonedAt(at);
        return cordon.floorIds().contains(bed.floorId()) || cordon.areaIds().contains(bed.areaId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Warning> warnings(CommandContext context) {
        if (context.command() != StatusCommand.RECORD_RETURN) return List.of();
        return read.list(context.residentId(), null, AllocationStatus.ACTIVE).stream()
                .filter(row -> blocked(row.location().bedId(), context.at()))
                .map(row -> new Warning(
                        "RETURNED_TO_ISOLATED_ROOM", "Phòng đang chịu chặn cách ly; bác sĩ và trưởng tầng sẽ được báo"))
                .toList();
    }

    private AllocationResidencySource source() {
        var source = residency.getIfAvailable();
        if (source == null)
            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "SOURCE_UNAVAILABLE", "Chưa có nguồn lưu trú");
        return source;
    }

    private static Optional<Long> replacement(JsonNode details) {
        if (details == null || !details.hasNonNull(SWITCH_BED)) return Optional.empty();
        return Optional.of(details.get(SWITCH_BED).longValue());
    }

    private static Basis basis(TransitionResult result) {
        return new Basis("FR-023/FR-025", null, result.historyId(), null);
    }

    private static Condition notReady() {
        return new Condition("RESERVED_BED_NOT_READY", "Giường đặt trước chưa sẵn sàng", "FR-023", false);
    }
}
