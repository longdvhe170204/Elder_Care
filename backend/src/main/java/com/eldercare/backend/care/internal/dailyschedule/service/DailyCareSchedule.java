package com.eldercare.backend.care.internal.dailyschedule.service;

import static com.eldercare.backend.care.internal.dailyschedule.domain.DailyRoutineKinds.DAILY_ROUTINES;

import com.eldercare.backend.care.api.DailyCareScheduleApi;
import com.eldercare.backend.care.api.DayCareAttendanceApi;
import com.eldercare.backend.care.api.dto.DailyScheduleEntry;
import com.eldercare.backend.care.api.dto.TaskStatus;
import com.eldercare.backend.care.api.spi.DailyMealPort;
import com.eldercare.backend.care.internal.catalog.persistence.CareCatalogStore;
import com.eldercare.backend.care.internal.dailyschedule.persistence.DailyScheduleStore;
import com.eldercare.backend.care.internal.session.persistence.SessionTimetableStore;
import com.eldercare.backend.care.internal.task.persistence.CareTaskStore;
import com.eldercare.backend.common.time.BusinessTime;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 005 FR-055 (Q-440): ghép giờ bữa (011), buổi hoạt động có tên hoặc mở cho toàn viện (014), mốc sinh hoạt cố định (FR-056)
 * và công việc chăm sóc của người cao tuổi, trừ Đã hủy; trạng thái rút gọn, không lý do, không liều thuốc. Người bán trú chỉ
 * hiện mục trong giờ có mặt.
 */
@Service
public class DailyCareSchedule implements DailyCareScheduleApi {
    public static final String MEAL = "MEAL", ACTIVITY = "ACTIVITY", ROUTINE = "ROUTINE", CARE_TASK = "CARE_TASK";

    private final CareTaskStore tasks;
    private final CareCatalogStore catalogs;
    private final SessionTimetableStore sessions;
    private final DailyScheduleStore store;
    private final ObjectProvider<DailyMealPort> meals;
    private final DayCareAttendanceApi attendance;

    public DailyCareSchedule(
            CareTaskStore tasks,
            CareCatalogStore catalogs,
            SessionTimetableStore sessions,
            DailyScheduleStore store,
            ObjectProvider<DailyMealPort> meals,
            DayCareAttendanceApi attendance) {
        this.tasks = tasks;
        this.catalogs = catalogs;
        this.sessions = sessions;
        this.store = store;
        this.meals = meals;
        this.attendance = attendance;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DailyScheduleEntry> onDate(long residentId, LocalDate date, boolean withMeasurements) {
        var from = date.atStartOfDay(BusinessTime.ZONE).toInstant();
        var to = date.plusDays(1).atStartOfDay(BusinessTime.ZONE).toInstant();
        var items = new ArrayList<DailyScheduleEntry>();
        for (var routine : catalogs.list(DAILY_ROUTINES))
            routine.attributes()
                    .on(date)
                    .filter(v -> v.active())
                    .ifPresent(v -> items.add(entry(at(date, v.time()), ROUTINE, v.name())));
        var port = meals.getIfAvailable();
        if (port != null)
            for (var meal : port.mealsOf(residentId, date)) items.add(entry(meal.at(), MEAL, meal.name()));
        var seen = new HashSet<String>();
        for (var s : sessions.registered(residentId, date))
            if (seen.add(s.at() + "|" + s.description())) items.add(entry(s.at(), ACTIVITY, s.description()));
        for (var s : store.facilitySessions(date))
            if (seen.add(s.at() + "|" + s.name())) items.add(entry(s.at(), ACTIVITY, s.name()));
        var measured =
                withMeasurements ? store.measuredValues(residentId, from, to) : Map.<Long, java.math.BigDecimal>of();
        for (var task : tasks.ofResidentBetween(residentId, from, to)) {
            var status = status(task.status());
            if (status == null) continue;
            var value = measured.get(task.id());
            items.add(new DailyScheduleEntry(
                    task.plannedAt(),
                    CARE_TASK,
                    task.data().taskTypeName(),
                    status,
                    value == null ? null : value.stripTrailingZeros().toPlainString()));
        }
        var window = presence(residentId, date);
        return items.stream()
                .filter(i -> window.map(w -> !i.at().isBefore(w[0]) && !i.at().isAfter(w[1]))
                        .orElse(true))
                .sorted(Comparator.comparing(DailyScheduleEntry::at)
                        .thenComparing(i -> order(i.kind()))
                        .thenComparing(DailyScheduleEntry::name, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    /** Hoàn thành, Chưa hoàn thành, Không thực hiện; Đã hủy không hiện. */
    static String status(TaskStatus status) {
        return switch (status) {
            case COMPLETED, COMPLETED_LATE -> "DONE";
            case NOT_PERFORMED -> "NOT_PERFORMED";
            case CANCELLED -> null;
            default -> "NOT_DONE";
        };
    }

    private Optional<Instant[]> presence(long residentId, LocalDate date) {
        return attendance.onDate(residentId, date).flatMap(a -> {
            var start = a.arrivedAt().or(() -> a.expectedArrival().map(t -> at(date, t)));
            var end = a.leftAt().or(() -> a.currentExpectedReturn().map(t -> at(date, t)));
            return start.isPresent() && end.isPresent()
                    ? Optional.of(new Instant[] {start.get(), end.get()})
                    : Optional.empty();
        });
    }

    private static int order(String kind) {
        return switch (kind) {
            case ROUTINE -> 0;
            case MEAL -> 1;
            case ACTIVITY -> 2;
            default -> 3;
        };
    }

    private static DailyScheduleEntry entry(Instant at, String kind, String name) {
        return new DailyScheduleEntry(at, kind, name, null, null);
    }

    private static Instant at(LocalDate date, LocalTime time) {
        return date.atTime(time).atZone(BusinessTime.ZONE).toInstant();
    }
}
