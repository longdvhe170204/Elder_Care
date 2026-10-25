package com.cambae.backend.m09.duty.internal.service;

import com.cambae.backend.common.Role;
import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.m09.api.ShiftApi;
import com.cambae.backend.m09.api.dto.*;
import com.cambae.backend.m09.shift.internal.domain.*;
import com.cambae.backend.m09.shift.internal.persistence.ShiftStore;
import com.cambae.backend.m09.staff.internal.persistence.StaffStore;
import com.cambae.backend.m15.account.api.AccountApi;
import com.cambae.backend.m15.account.api.dto.AccountStatus;
import com.cambae.backend.m15.account.api.spi.*;
import com.cambae.backend.m15.parameter.api.*;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** T015: truy vấn theo thời điểm, giờ ca là nguồn xác định phạm vi, không chờ nhịp job. */
@Service
@Transactional(readOnly = true)
public class ShiftQueries implements ShiftApi {
    public record OfficeHours(LocalTime from, LocalTime to, Set<DayOfWeek> days) {}

    private final ShiftStore store;
    private final StaffStore staff;
    private final AccountApi accounts;
    private final LicenseApi licenses;
    private final ParameterApi parameters;
    private final BusinessTime time;

    public ShiftQueries(
            ShiftStore store,
            StaffStore staff,
            AccountApi accounts,
            LicenseApi licenses,
            ParameterApi parameters,
            BusinessTime time) {
        this.store = store;
        this.staff = staff;
        this.accounts = accounts;
        this.licenses = licenses;
        this.parameters = parameters;
        this.time = time;
    }

    public Optional<ShiftView> currentShift(long floorAreaId, Instant at) {
        return atFloor(floorAreaId, at).map(s -> view(s, at));
    }

    public List<ShiftView> staffShifts(long staffId, Instant from, Instant to) {
        return store.staffRange(staffId, from, to, true).stream()
                .map(s -> view(s, time.now()))
                .toList();
    }

    public boolean onShift(long staffId, Instant at) {
        var account = accounts.byStaff(staffId);
        if (account.isPresent()
                && !Collections.disjoint(
                        account.get().roles(), Set.of(Role.NURSING_HOME_MANAGER, Role.OFFICE_STAFF, Role.ACCOUNTANT))) {
            var hours = parameters.read(ParameterCode.CFG_M13_06).asTable(OfficeHours.class);
            var local = at.atZone(BusinessTime.ZONE);
            return hours.days().contains(local.getDayOfWeek())
                    && !local.toLocalTime().isBefore(hours.from())
                    && local.toLocalTime().isBefore(hours.to());
        }
        return inProgressMember(staffId, at);
    }

    public Optional<StaffPublicView> staff(long id) {
        return staff.find(id)
                .map(s -> new StaffPublicView(
                        s.id(), s.fullName(), s.position(), s.specialty(), StaffStatus.valueOf(s.status())));
    }

    public Optional<WorkShift> atFloor(long floor, Instant at) {
        var found = store.atFloor(floor, at);
        if (found.size() > 1) throw new InvalidPermissionDataException("M09.SHIFT", "MULTIPLE_CURRENT_SHIFTS", floor);
        return found.stream().findFirst();
    }

    public Optional<WorkShift> next(long floor, Instant at) {
        return store.nextAtFloor(floor, at);
    }

    public Optional<Long> leadAtFloor(long floor, Instant at) {
        return atFloor(floor, at).flatMap(shift -> {
            var people = store.leadStaffAt(shift.id(), at);
            if (people.size() > 1)
                throw new InvalidPermissionDataException("M09.SHIFT", "MULTIPLE_SHIFT_LEADS", shift.id());
            return people.stream().findFirst();
        });
    }

    public List<WorkShift> published(long floor, Instant from, Instant to) {
        return store.floorRange(floor, from, to);
    }

    public Optional<WorkShift> find(long id) {
        return store.find(id);
    }

    public List<WorkShift> locationReferences(String type, long id, boolean activeOnly) {
        return store.locationReferences(type, id, activeOnly);
    }

    public boolean published(WorkShift shift) {
        return !Set.of("DRAFT", "CANCELLED").contains(shift.status());
    }

    public boolean inProgressMember(long staffId, Instant at) {
        return !store.memberAt(staffId, at).isEmpty();
    }

    public boolean listed(long staffId, long shiftId) {
        return store.members(shiftId).stream().anyMatch(m -> m.staffId() == staffId);
    }

    public List<Long> nurses(long floor, Instant at) {
        return atFloor(floor, at).stream()
                .flatMap(s -> store.members(s.id()).stream().filter(m -> m.presentAt(at)))
                .map(ShiftMember::staffId)
                .filter(id -> hasRole(id, Role.NURSING_STAFF))
                .toList();
    }

    public List<Long> doctors(Instant at) {
        return store.at(at, true).stream()
                .flatMap(s -> store.members(s.id()).stream()
                        .filter(ShiftMember::onDutyDoctor)
                        .filter(m -> m.presentAt(at)))
                .map(ShiftMember::staffId)
                .filter(id -> hasRole(id, Role.DOCTOR))
                .distinct()
                .toList();
    }

    public List<Long> floors(Instant at) {
        return store.at(at, false).stream()
                .map(WorkShift::scopeId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    public Optional<Long> floor(long staffId, Instant at) {
        return store.memberAt(staffId, at).stream()
                .map(WorkShift::scopeId)
                .filter(Objects::nonNull)
                .findFirst();
    }

    public boolean qualified(long staffId, Set<String> certificates, LocalDate date) {
        if (staff.find(staffId).filter(s -> "WORKING".equals(s.status())).isEmpty()
                || accounts.byStaff(staffId)
                        .filter(a -> a.status() != AccountStatus.LOCKED)
                        .isEmpty()) return false;
        return certificates.stream()
                .allMatch(type -> licenses.valid(staffId, type, date).isPresent());
    }

    public boolean anyQualified(long shiftId, Role role, Set<String> certificates) {
        return find(shiftId).stream()
                .anyMatch(s -> store.members(shiftId).stream()
                        .anyMatch(m -> hasRole(m.staffId(), role)
                                && m.presentAt(s.startsAt())
                                && qualified(
                                        m.staffId(),
                                        certificates,
                                        LocalDate.ofInstant(s.startsAt(), BusinessTime.ZONE))));
    }

    private boolean hasRole(long staffId, Role role) {
        return accounts.byStaff(staffId).map(a -> a.roles().contains(role)).orElse(false);
    }

    private ShiftView view(WorkShift shift, Instant at) {
        String status = shift.status();
        if (published(shift) && !shift.startsAt().isAfter(at) && shift.endsAt().isAfter(at)) status = "IN_PROGRESS";
        return new ShiftView(
                shift.id(),
                shift.scopeId(),
                shift.startsAt(),
                shift.endsAt(),
                status,
                shift.missingLead() ? null : shift.leadStaffId());
    }
}
