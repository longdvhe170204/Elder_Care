package com.eldercare.backend.roombedallocation.internal.history.service;

import com.eldercare.backend.common.error.InvalidValueException;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.roombedallocation.api.LocationHistoryApi;
import com.eldercare.backend.roombedallocation.api.dto.*;
import com.eldercare.backend.roombedallocation.internal.history.persistence.LocationHistoryStore;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * API sự kiện/vị trí cho 007, 016 như ResidentLocationApi: module gọi chịu trách nhiệm quyền của chức năng gọi.
 * HTTP M03 chỉ đi qua LocationHistoryQueries có kiểm quyền và historyFilter; không expose API đọc nguồn này qua web.
 */
@Service
@Transactional(readOnly = true)
public class LocationHistoryReading implements LocationHistoryApi {
    private final LocationHistoryStore store;
    private final LocationHistoryQueries facts;
    private final BusinessTime time;

    public LocationHistoryReading(LocationHistoryStore store, LocationHistoryQueries facts, BusinessTime time) {
        this.store = store;
        this.facts = facts;
        this.time = time;
    }

    @Override
    public List<ResidentLocationRow> byResident(long id, Instant from, Instant to) {
        window(from, to);
        var rows = store.resident(id, from, to);
        if (rows.isEmpty()) return List.of();
        String name = facts.name(id);
        boolean pending = facts.pending(id, from, to);
        return rows.stream()
                .map(row -> new ResidentLocationRow(
                        id, name, row.location(), row.from(), row.to(), row.corrected(), pending, List.of()))
                .toList();
    }

    @Override
    public List<BedLocationRow> byLocation(LocationRef location, Instant at) {
        if (at == null) throw new InvalidValueException("Cần thời điểm tra cứu");
        return bedRows(location, at, at, true);
    }

    @Override
    public List<BedLocationRow> byLocationOn(LocationRef location, LocalDate date) {
        if (date == null) throw new InvalidValueException("Cần ngày tra cứu");
        return bedRows(location, time.startOfDay(date), time.startOfDay(date.plusDays(1)), false);
    }

    private List<BedLocationRow> bedRows(LocationRef location, Instant from, Instant to, boolean point) {
        Map<Long, String> names = new HashMap<>();
        Map<Long, Boolean> pending = new HashMap<>();
        return store.location(location, from, to, point).stream()
                .map(row -> new BedLocationRow(
                        row.residentId(),
                        names.computeIfAbsent(row.residentId(), facts::name),
                        row.location(),
                        row.from(),
                        row.to(),
                        row.corrected(),
                        pending.computeIfAbsent(
                                row.residentId(), id -> facts.pending(id, from, point ? from.plusNanos(1000) : to)),
                        List.of()))
                .toList();
    }

    @Override
    public List<RoommateView> roommates(long id, Instant from, Instant to) {
        window(from, to);
        Map<Long, String> names = new HashMap<>();
        Map<Long, Boolean> pending = new HashMap<>();
        Map<Long, List<AbsenceInterval>> absent = new HashMap<>();
        return store.roommates(id, from, to).stream()
                .map(row -> {
                    List<AbsenceInterval> periods = new ArrayList<>();
                    for (long person : List.of(id, row.residentId()))
                        for (var period : absent.computeIfAbsent(person, key -> facts.absences(key, from, to))) {
                            Instant start = period.from().isBefore(row.from()) ? row.from() : period.from();
                            Instant end = period.to().isAfter(row.to()) ? row.to() : period.to();
                            if (start.isBefore(end)) periods.add(new AbsenceInterval(start, end));
                        }
                    return new RoommateView(
                            row.residentId(),
                            names.computeIfAbsent(row.residentId(), facts::name),
                            row.location(),
                            row.from(),
                            row.to(),
                            row.corrected(),
                            pending.computeIfAbsent(id, key -> facts.pending(key, from, to))
                                    || pending.computeIfAbsent(row.residentId(), key -> facts.pending(key, from, to)),
                            List.copyOf(periods));
                })
                .toList();
    }

    private static void window(Instant from, Instant to) {
        if (from == null || to == null || !from.isBefore(to))
            throw new InvalidValueException("Khoảng tra cứu phải có từ trước đến");
    }
}
