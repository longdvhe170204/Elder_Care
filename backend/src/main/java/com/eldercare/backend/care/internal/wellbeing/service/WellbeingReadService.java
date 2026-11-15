package com.eldercare.backend.care.internal.wellbeing.service;

import com.eldercare.backend.administration.account.api.RequiresPermission;
import com.eldercare.backend.administration.account.api.dto.*;
import com.eldercare.backend.administration.correction.api.CorrectionApi;
import com.eldercare.backend.administration.shared.api.RecordEventApi;
import com.eldercare.backend.administration.shared.api.SimpleRecordApi;
import com.eldercare.backend.administration.shared.api.dto.EntityRef;
import com.eldercare.backend.care.api.TaskStatisticsApi;
import com.eldercare.backend.care.api.dto.ResultType;
import com.eldercare.backend.care.internal.session.domain.SessionKinds;
import com.eldercare.backend.care.internal.trip.domain.TripKinds;
import com.eldercare.backend.care.internal.wellbeing.domain.*;
import com.eldercare.backend.care.internal.wellbeing.persistence.WellbeingStore;
import com.eldercare.backend.common.error.InvalidValueException;
import com.eldercare.backend.common.time.BusinessTime;
import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** FR-051, FR-053: tổng hợp từ bản ghi hiện hành, gồm đính chính và lượt rời sớm. */
@Service
public class WellbeingReadService {
    private final SimpleRecordApi records;
    private final RecordEventApi events;
    private final CorrectionApi corrections;
    private final WellbeingStore store;
    private final TaskStatisticsApi tasks;
    private final BusinessTime time;

    public WellbeingReadService(
            SimpleRecordApi records,
            RecordEventApi events,
            CorrectionApi corrections,
            WellbeingStore store,
            TaskStatisticsApi tasks,
            BusinessTime time) {
        this.records = records;
        this.events = events;
        this.corrections = corrections;
        this.store = store;
        this.tasks = tasks;
        this.time = time;
    }

    @RequiresPermission(function = Function.ACTIVITY_OUTING, operation = Operation.VIEW)
    @Transactional(readOnly = true)
    public PreferencesView preferences(Target target, long residentId) {
        return preferencesOf(residentId);
    }

    private PreferencesView preferencesOf(long residentId) {
        var rows = records.listByResident(Preference.KIND, residentId, Set.of()).stream()
                .map(PreferencesView.Item::of)
                .sorted(Comparator.comparing(
                                (PreferencesView.Item p) -> p.source().type().ordinal())
                        .thenComparing(PreferencesView.Item::recordedAt)
                        .thenComparingLong(PreferencesView.Item::id))
                .toList();
        return new PreferencesView(
                rows.stream().filter(p -> Preference.CURRENT.equals(p.status())).toList(),
                rows.stream()
                        .filter(p -> !Preference.CURRENT.equals(p.status()))
                        .toList());
    }

    @RequiresPermission(function = Function.ACTIVITY_OUTING, operation = Operation.VIEW)
    @Transactional(readOnly = true)
    public WellbeingProfile profile(Target target, long residentId, LocalDate from, LocalDate to) {
        if (from == null || to == null || to.isBefore(from))
            throw new InvalidValueException("Khoảng ngày không hợp lệ");
        var sessions = store.sessions(residentId, time.startOfDay(from), time.startOfDay(to.plusDays(1)));
        var parent = new EntityRef("M01.RESIDENT", residentId);
        var attendance = events.list(SessionKinds.ATTENDANCE, parent);
        var current = corrections.current(
                SessionKinds.ATTENDANCE.code(),
                attendance.stream().map(r -> r.id()).toList());
        var bySession = new LinkedHashMap<Long, WellbeingProfile.Session>();
        for (var row : attendance) {
            var a = row.content();
            var session = sessions.get(a.sessionId());
            var value = current.get(row.id());
            if (session == null || (value != null && value.voided())) continue;
            String result = a.result(), participation = a.participationLevel(), communication = a.communicationLevel();
            if (value != null && value.corrected() && value.content() instanceof Map<?, ?> map) {
                result = text(map.get("result"));
                participation = text(map.get("participationLevel"));
                communication = text(map.get("communicationLevel"));
            }
            bySession.put(
                    a.sessionId(),
                    new WellbeingProfile.Session(
                            a.sessionId(), session.startsAt(), result, participation, communication));
        }
        var departures = events.list(TripKinds.ATTENDANCE, parent);
        var tripCurrent = corrections.current(
                TripKinds.ATTENDANCE.code(),
                departures.stream().map(r -> r.id()).toList());
        for (var row : departures) {
            var a = row.content();
            var session = sessions.get(a.tripId());
            var value = tripCurrent.get(row.id());
            if (session == null || !"DEPARTURE".equals(a.type()) || (value != null && value.voided())) continue;
            bySession.put(
                    a.tripId(), new WellbeingProfile.Session(a.tripId(), session.startsAt(), "PRESENT", null, null));
        }
        var weeks = new TreeMap<LocalDate, Integer>();
        for (var s : bySession.values()) {
            if (!sessions.get(s.sessionId()).groupActivity() || !"PRESENT".equals(s.result())) continue;
            var week = LocalDate.ofInstant(s.startsAt(), BusinessTime.ZONE)
                    .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            weeks.merge(week, 1, Integer::sum);
        }
        var days = new TreeMap<LocalDate, List<WellbeingProfile.Observation>>();
        try (var results = tasks.resultsInRange(
                residentId,
                Set.of(ResultType.MOOD, ResultType.BEHAVIOR),
                time.startOfDay(from),
                time.startOfDay(to.plusDays(1)))) {
            results.forEach(r -> days.computeIfAbsent(
                            LocalDate.ofInstant(r.performedAt(), BusinessTime.ZONE), k -> new ArrayList<>())
                    .add(new WellbeingProfile.Observation(
                            r.id(),
                            r.resultType().name(),
                            r.performedAt(),
                            r.values(),
                            r.note().orElse(null))));
        }
        return new WellbeingProfile(
                from,
                to,
                weeks.entrySet().stream()
                        .map(e -> new WellbeingProfile.Week(e.getKey(), e.getValue()))
                        .toList(),
                bySession.values().stream()
                        .sorted(Comparator.comparing(WellbeingProfile.Session::startsAt)
                                .thenComparingLong(WellbeingProfile.Session::sessionId))
                        .toList(),
                days.entrySet().stream()
                        .map(e -> new WellbeingProfile.Day(e.getKey(), e.getValue()))
                        .toList(),
                preferencesOf(residentId),
                null);
    }

    private static String text(Object value) {
        return value == null ? null : value.toString();
    }
}
