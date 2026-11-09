package com.eldercare.backend.care.internal.timetable.service;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.RequiresPermission;
import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.care.api.DayCareAttendanceApi;
import com.eldercare.backend.care.api.TimetableApi;
import com.eldercare.backend.care.api.spi.TimetableSourcePort;
import com.eldercare.backend.care.api.spi.TimetableSourcePort.TimetableItemView;
import com.eldercare.backend.care.internal.task.persistence.CareTaskStore;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.error.PermissionDeniedException;
import com.eldercare.backend.common.time.BusinessTime;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** T058 (FR-048): gộp công việc, nguồn 006, 012, 014 và lịch đến/về bán trú; chỉ đọc, không có lệnh sửa. */
@Service
public class CareTimetable implements TimetableApi {
    private static final String DAY_CARE = "DAY_CARE_ARRIVAL_DEPARTURE";

    private final CareTaskStore tasks;
    private final List<TimetableSourcePort> sources;
    private final DayCareAttendanceApi attendance;
    private final CurrentUser user;

    public CareTimetable(
            CareTaskStore tasks, List<TimetableSourcePort> sources, DayCareAttendanceApi attendance, CurrentUser user) {
        this.tasks = tasks;
        this.sources = sources;
        this.attendance = attendance;
        this.user = user;
    }

    /** Trưởng tầng và Điều dưỡng trong phạm vi (FR-048). */
    @RequiresPermission(function = Function.CARE_PLAN, operation = Operation.VIEW)
    @Transactional(readOnly = true)
    public List<TimetableItemView> view(Target target, long residentId, LocalDate date) {
        if (user.roles().stream().noneMatch(r -> r == Role.FLOOR_MANAGER || r == Role.NURSING_STAFF))
            throw new PermissionDeniedException("Chỉ Trưởng tầng và Điều dưỡng xem thời khóa biểu");
        return onDate(residentId, date);
    }

    public static Target target(long residentId) {
        return new Target("M04.TIMETABLE", Long.toString(residentId), residentId, null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TimetableItemView> onDate(long residentId, LocalDate date) {
        var from = date.atStartOfDay(BusinessTime.ZONE).toInstant();
        var to = date.plusDays(1).atStartOfDay(BusinessTime.ZONE).toInstant();
        var items = new ArrayList<TimetableItemView>();
        for (var task : tasks.ofResidentBetween(residentId, from, to))
            items.add(new TimetableItemView(
                    task.plannedAt(),
                    "CARE_TASK",
                    task.data().content(),
                    task.status().name()));
        for (var source : sources) items.addAll(source.onDate(residentId, date));
        attendance.onDate(residentId, date).ifPresent(a -> {
            var status = a.status().name();
            at(date, a.arrivedAt(), a.expectedArrival())
                    .ifPresent(at -> items.add(new TimetableItemView(at, DAY_CARE, "Giờ đến", status)));
            at(date, a.leftAt(), a.currentExpectedReturn())
                    .ifPresent(at -> items.add(new TimetableItemView(at, DAY_CARE, "Giờ về", status)));
        });
        items.sort(Comparator.comparing(TimetableItemView::at));
        return List.copyOf(items);
    }

    private static Optional<Instant> at(LocalDate date, Optional<Instant> actual, Optional<LocalTime> expected) {
        return actual.or(
                () -> expected.map(t -> date.atTime(t).atZone(BusinessTime.ZONE).toInstant()));
    }
}
