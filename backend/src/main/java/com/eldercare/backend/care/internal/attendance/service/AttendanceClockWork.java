package com.eldercare.backend.care.internal.attendance.service;

import com.eldercare.backend.administration.parameter.api.*;
import com.eldercare.backend.care.api.dto.AttendanceStatus;
import com.eldercare.backend.care.api.events.*;
import com.eldercare.backend.care.internal.attendance.persistence.AttendanceStore;
import com.eldercare.backend.care.internal.scheduling.service.CareMinuteWork;
import com.eldercare.backend.care.internal.status.persistence.CareLockStore;
import com.eldercare.backend.common.data.ReminderMarkers;
import com.eldercare.backend.common.notification.*;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.resident.api.ResidentApi;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import java.time.*;
import java.util.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.*;

/** T030: chạy bù theo mốc của dữ liệu; chỉ đổi trạng thái và nhắc, không tự điểm danh. */
@Component
@Transactional(isolation = Isolation.READ_COMMITTED)
public class AttendanceClockWork implements CareMinuteWork {
    private final AttendanceStore store;
    private final AttendanceService commands;
    private final CareLockStore locks;
    private final ParameterApi parameters;
    private final ReminderMarkers markers;
    private final ApplicationEventPublisher events;
    private final ResidentApi residents;

    public AttendanceClockWork(
            AttendanceStore store,
            AttendanceService commands,
            CareLockStore locks,
            ParameterApi parameters,
            ReminderMarkers markers,
            ApplicationEventPublisher events,
            ResidentApi residents) {
        this.store = store;
        this.commands = commands;
        this.locks = locks;
        this.parameters = parameters;
        this.markers = markers;
        this.events = events;
        this.residents = residents;
    }

    public int run(Instant now) {
        int count = 0;
        var today = now.atZone(BusinessTime.ZONE).toLocalDate();
        var candidates = store.all();
        locks.lock(candidates.stream().map(r -> r.residentId()).toList());
        for (var candidate : candidates) {
            var row = store.lock(candidate.id());
            if (!row.status().equals("NOT_ARRIVED")
                    || row.content().expectedArrival() == null
                    || residents.status(row.residentId()) != ResidentStatus.IN_RESIDENCE) continue;
            commands.refresh(row.residentId(), row.effectiveFrom());
            row = store.lock(row.id());
            if (!row.status().equals("NOT_ARRIVED") || row.content().expectedArrival() == null) continue;
            var limit = row.effectiveFrom()
                    .atTime(row.content().expectedArrival())
                    .atZone(BusinessTime.ZONE)
                    .toInstant()
                    .plus(parameters.read(ParameterCode.CFG_M02_07).asDuration());
            if (now.isBefore(limit)) continue;
            var after = store.change(row, AttendanceStatus.UNREPORTED_ABSENT, row.content());
            commands.log(row, after, "UNREPORTED_ABSENCE", now);
            events.publishEvent(new UnreportedAbsence(
                    row.residentId(),
                    row.effectiveFrom(),
                    commands.context(
                            after,
                            "NO_SHOW",
                            now,
                            List.of(new RecipientGroup.FloorManager(
                                    row.content().floorAreaId(), new Substitution.Default())))));
            count++;
        }
        // Dùng đúng snapshot đã khóa, không lấy thêm người có id nhỏ hơn giữa transaction.
        for (var candidate : candidates) count += remind(candidate.id(), now);
        return count;
    }

    public int endDay(LocalDate date, Instant now) {
        if (!date.isBefore(now.atZone(BusinessTime.ZONE).toLocalDate())) return 0;
        var candidates = store.onDate(date);
        locks.lock(candidates.stream().map(r -> r.residentId()).toList());
        int count = 0;
        for (var candidate : candidates) count += remind(candidate.id(), now);
        return count;
    }

    /** Caller đã khóa resident tăng dần trước khi khóa dòng. */
    private int remind(long id, Instant now) {
        var row = store.lock(id);
        if (!row.status().equals("PRESENT")
                || !row.effectiveFrom().isBefore(now.atZone(BusinessTime.ZONE).toLocalDate())
                || residents.isFinalStatus(row.residentId())) return 0;
        var date = row.effectiveFrom();
        if (!markers.mark(
                "M04.ATTENDANCE_DAY_END",
                "M04.DAY_CARE_ATTENDANCE",
                row.id(),
                date.toString(),
                Map.of("date", date.toString()))) return 0;
        var recipients = List.<RecipientGroup>of(
                new RecipientGroup.ShiftLead(row.content().floorAreaId(), new Substitution.Default()),
                new RecipientGroup.FloorManager(row.content().floorAreaId(), new Substitution.Default()));
        events.publishEvent(
                new DayCarePresentAtDayEnd(row.residentId(), date, commands.context(row, "DAY_END", now, recipients)));
        return 1;
    }
}
