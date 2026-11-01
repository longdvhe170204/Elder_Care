package com.eldercare.backend.care.internal.attendance.service;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.PermissionApi;
import com.eldercare.backend.administration.account.api.RequiresPermission;
import com.eldercare.backend.administration.account.api.dto.*;
import com.eldercare.backend.administration.shared.api.dto.SimpleRecord;
import com.eldercare.backend.care.api.DayCareAttendanceApi;
import com.eldercare.backend.care.api.dto.*;
import com.eldercare.backend.care.internal.attendance.domain.AttendanceData;
import com.eldercare.backend.care.internal.attendance.persistence.AttendanceStore;
import com.eldercare.backend.common.error.PermissionDeniedException;
import java.time.*;
import java.util.*;
import org.springframework.context.annotation.Fallback;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
@Fallback
@Transactional(readOnly = true)
public class AttendanceQueries implements DayCareAttendanceApi {
    private final AttendanceStore store;
    private final AttendanceService commands;
    private final CurrentUser user;
    private final PermissionApi permissions;

    public AttendanceQueries(
            AttendanceStore store, AttendanceService commands, CurrentUser user, PermissionApi permissions) {
        this.store = store;
        this.commands = commands;
        this.user = user;
        this.permissions = permissions;
    }

    @RequiresPermission(function = Function.DAY_CARE_ATTENDANCE, operation = Operation.VIEW)
    public List<SimpleRecord<AttendanceData>> list(Target target, LocalDate date) {
        return store.onDate(date).stream()
                .filter(r -> permissions
                        .hasPermission(
                                user,
                                new PermissionKey(Function.DAY_CARE_ATTENDANCE, Operation.VIEW),
                                commands.target(r.id()))
                        .allowed())
                .toList();
    }

    public static AttendanceView view(SimpleRecord<AttendanceData> row) {
        var d = row.content();
        return new AttendanceView(
                row.id(),
                row.residentId(),
                row.effectiveFrom(),
                AttendanceStatus.valueOf(row.status()),
                Optional.ofNullable(d.expectedArrival()),
                Optional.ofNullable(d.expectedReturn()),
                Optional.ofNullable(d.currentExpectedReturn()),
                Optional.ofNullable(d.arrivedAt()),
                Optional.ofNullable(d.leftAt()),
                d.adHocSession());
    }

    @Override
    public Optional<AttendanceView> onDate(long resident, LocalDate date) {
        return store.find(resident, date).map(AttendanceQueries::view);
    }

    @Override
    public Optional<LocalTime> returnTimeOn(long resident, LocalDate date) {
        return onDate(resident, date).flatMap(AttendanceView::currentExpectedReturn);
    }

    @Override
    public List<AttendanceView> inRange(long resident, LocalDate from, LocalDate to) {
        return store.forResident(resident).stream()
                .filter(r ->
                        !r.effectiveFrom().isBefore(from) && !r.effectiveFrom().isAfter(to))
                .sorted(Comparator.comparing(SimpleRecord::effectiveFrom))
                .map(AttendanceQueries::view)
                .toList();
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void refreshCalendar(long resident, LocalDate date) {
        commands.refresh(resident, date);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void changeReturnTime(long resident, LocalDate date, LocalTime time, long registration) {
        commands.returnTime(resident, date, time, registration, false);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void restoreReturnTime(long resident, LocalDate date, long registration) {
        commands.returnTime(resident, date, null, registration, true);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void checkOut(long resident, LocalDate date, long pickup, CheckoutType type, Instant at, CurrentUser actor) {
        if (actor.accountId() != user.accountId())
            throw new PermissionDeniedException("Người thực hiện không khớp phiên");
        var row = store.find(resident, date).orElseThrow(com.eldercare.backend.common.error.NotFoundException::new);
        var result = commands.checkOut(commands.target(row.id()), row.id(), pickup, at);
        if (result.attendance().content().checkoutType() != type)
            throw new com.eldercare.backend.common.error.InvalidValueException("Loại bàn giao không khớp bản ghi đón");
    }
}
