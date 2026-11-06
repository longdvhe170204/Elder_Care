package com.eldercare.backend.staffing.internal.handover.service;

import com.eldercare.backend.administration.account.api.AccountApi;
import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.dto.*;
import com.eldercare.backend.care.api.spi.ShiftStaffPort.HandoverTasksView;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.staffing.internal.handover.domain.NextHandoverShift;
import com.eldercare.backend.staffing.internal.handover.persistence.HandoverStore;
import com.eldercare.backend.staffing.internal.shift.domain.WorkShift;
import com.eldercare.backend.staffing.internal.shift.service.ShiftHandoverState;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;

/** T026: nguồn thật cho vòng đời ca; Có ý kiến vẫn là chưa lập xong (BR-M09-07). */
@Service
public class HandoverQueries implements ShiftHandoverState {
    private final HandoverStore store;
    private final com.eldercare.backend.staffing.internal.shift.persistence.ShiftStore shifts;
    private final com.eldercare.backend.administration.parameter.api.ParameterApi parameters;
    private final com.eldercare.backend.common.time.BusinessTime time;
    private final AccountApi accounts;
    private final HandoverAccess access;

    public HandoverQueries(
            HandoverStore store,
            com.eldercare.backend.staffing.internal.shift.persistence.ShiftStore shifts,
            com.eldercare.backend.administration.parameter.api.ParameterApi parameters,
            com.eldercare.backend.common.time.BusinessTime time,
            AccountApi accounts,
            HandoverAccess access) {
        this.store = store;
        this.shifts = shifts;
        this.parameters = parameters;
        this.time = time;
        this.accounts = accounts;
        this.access = access;
    }

    public HandoverTasksView tasks(long staffId, long shiftId) {
        var shift = shifts.find(shiftId);
        var account = accounts.byStaff(staffId).filter(a -> a.status() == AccountStatus.ACTIVE);
        if (shift.isEmpty() || account.isEmpty()) return new HandoverTasksView(false, false);
        var s = shift.get();
        if (!s.handoverRequired() || Set.of("DRAFT", "CANCELLED").contains(s.status()))
            return new HandoverTasksView(false, false);
        var user = new Participant(
                account.get().accountId(), staffId, account.get().roles());
        var status = store.statusForShift(shiftId);
        boolean prepare = !time.now().isBefore(access.preparationStarts(s))
                && status.filter(Set.of("PREPARED", "CONFIRMED")::contains).isEmpty()
                && access.sender(s, user);
        boolean receive = access.receiver(s, user) && preparedPreviousFor(s);
        return new HandoverTasksView(receive, prepare);
    }

    private boolean preparedPreviousFor(WorkShift receiving) {
        var gap = parameters
                .read(com.eldercare.backend.administration.parameter.api.dto.ParameterCode.CFG_M09_09)
                .asDuration();
        for (long id : store.unconfirmedPrevious(receiving.startsAt().minus(gap), receiving.startsAt())) {
            var previous = shifts.shift(id, false);
            if (!previous.scopeType().equals(receiving.scopeType())
                    || !java.util.Objects.equals(previous.scopeId(), receiving.scopeId())
                    || !store.statusForShift(id).filter("PREPARED"::equals).isPresent()) continue;
            var next = NextHandoverShift.find(
                    previous,
                    shifts.floorRange(
                            previous.scopeId(),
                            previous.startsAt(),
                            previous.endsAt().plus(gap).plusNanos(1000)),
                    gap);
            if (next.filter(s -> s.id() == receiving.id()).isPresent()) return true;
        }
        return false;
    }

    /** Chỉ hỏi quyền của tài khoản nhân viên, không phải phiên đăng nhập hoặc người thực hiện lệnh. */
    private record Participant(long accountId, long staff, Set<Role> roles) implements CurrentUser {
        public AccountType type() {
            return AccountType.STAFF;
        }

        public Optional<Long> staffId() {
            return Optional.of(staff);
        }

        public Optional<Long> familyMemberId() {
            return Optional.empty();
        }

        public Optional<Long> residentId() {
            return Optional.empty();
        }

        public boolean isScheduler() {
            return false;
        }

        public String deviceId() {
            throw new IllegalStateException("Truy vấn checklist không có phiên");
        }

        public AccessChannel channel() {
            throw new IllegalStateException("Truy vấn checklist không có phiên");
        }
    }

    @Override
    public boolean prepared(long shiftId) {
        return store.statusForShift(shiftId)
                .filter(Set.of("PREPARED", "CONFIRMED")::contains)
                .isPresent();
    }

    public boolean confirmed(long shiftId) {
        return store.statusForShift(shiftId).filter("CONFIRMED"::equals).isPresent();
    }

    public java.util.List<com.eldercare.backend.incident.api.dto.UnconfirmedShiftHandoverView> startedUnconfirmed(
            java.time.Instant from, java.time.Instant to) {
        var gap = parameters
                .read(com.eldercare.backend.administration.parameter.api.dto.ParameterCode.CFG_M09_09)
                .asDuration();
        var result = new java.util.ArrayList<com.eldercare.backend.incident.api.dto.UnconfirmedShiftHandoverView>();
        for (long id : store.unconfirmedPrevious(from.minus(gap), to)) {
            var previous = shifts.shift(id, false);
            var next = com.eldercare.backend.staffing.internal.handover.domain.NextHandoverShift.find(
                    previous,
                    shifts.floorRange(
                            previous.scopeId(),
                            previous.startsAt(),
                            previous.endsAt().plus(gap).plusNanos(1000)),
                    gap);
            next.filter(s -> s.startsAt().isAfter(from)
                            && !s.startsAt().isAfter(to)
                            && !s.startsAt().isAfter(time.now()))
                    .ifPresent(s -> result.add(new com.eldercare.backend.incident.api.dto.UnconfirmedShiftHandoverView(
                            previous.id(), s.id(), previous.scopeId())));
        }
        return java.util.List.copyOf(result);
    }
}
