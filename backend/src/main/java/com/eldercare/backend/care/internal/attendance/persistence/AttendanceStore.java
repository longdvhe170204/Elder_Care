package com.eldercare.backend.care.internal.attendance.persistence;

import com.eldercare.backend.administration.shared.api.*;
import com.eldercare.backend.administration.shared.api.dto.*;
import com.eldercare.backend.care.api.dto.*;
import com.eldercare.backend.care.internal.attendance.domain.*;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Repository;

/** Chỉ dùng API bảng chung; con cùng trỏ hồ sơ người để R-15 loại bỏ toàn chuỗi khi hết hạn. */
@Repository
public class AttendanceStore {
    private final SimpleRecordApi rows;
    private final RecordEventApi events;

    public AttendanceStore(SimpleRecordApi rows, RecordEventApi events) {
        this.rows = rows;
        this.events = events;
    }

    public String key(long resident, LocalDate date) {
        return "M04.DAY_CARE_ATTENDANCE:" + resident + ":" + date;
    }

    public Optional<SimpleRecord<AttendanceData>> find(long resident, LocalDate date) {
        return rows.findByUniqueKey(AttendanceKinds.DAYS, key(resident, date));
    }

    public SimpleRecord<AttendanceData> require(long id) {
        return rows.require(AttendanceKinds.DAYS, id);
    }

    public SimpleRecord<AttendanceData> lock(long id) {
        return rows.lockForUpdate(AttendanceKinds.DAYS, id);
    }

    public List<SimpleRecord<AttendanceData>> all() {
        return rows.listByStatus(
                AttendanceKinds.DAYS,
                Arrays.stream(AttendanceStatus.values()).map(Enum::name).collect(java.util.stream.Collectors.toSet()));
    }

    public List<SimpleRecord<AttendanceData>> onDate(LocalDate date) {
        return all().stream().filter(r -> date.equals(r.effectiveFrom())).toList();
    }

    public List<SimpleRecord<AttendanceData>> forResident(long resident) {
        return rows.listByResident(AttendanceKinds.DAYS, resident, Set.of());
    }

    public SimpleRecord<AttendanceData> create(
            long resident, LocalDate date, AttendanceStatus status, AttendanceData data, long actor) {
        return rows.create(
                AttendanceKinds.DAYS,
                new NewSimpleRecord<>(
                        resident,
                        new EntityRef("M01.RESIDENT", resident),
                        status.name(),
                        data,
                        date,
                        null,
                        key(resident, date),
                        actor,
                        false));
    }

    public SimpleRecord<AttendanceData> change(
            SimpleRecord<AttendanceData> row, AttendanceStatus status, AttendanceData data) {
        return rows.change(
                        AttendanceKinds.DAYS,
                        row.id(),
                        row.version(),
                        new RecordChange<>(status.name(), data, null, row.uniqueKey(), false))
                .after();
    }

    public RecordEvent<AttendanceRecordData> add(
            SimpleRecord<AttendanceData> row,
            String direction,
            Instant at,
            CheckoutType type,
            Long pickup,
            long actor,
            Instant recordedAt) {
        return events.add(
                AttendanceKinds.RECORDS,
                new EntityRef("M01.RESIDENT", row.residentId()),
                new NewRecordEvent<>(
                        new AttendanceRecordData(row.id(), direction, at, type, pickup),
                        null,
                        actor,
                        false,
                        recordedAt,
                        null,
                        recordedAt,
                        row.content().floorAreaId(),
                        null));
    }

    public RecordEvent<AttendanceRecordData> record(long id) {
        return events.require(AttendanceKinds.RECORDS, id);
    }
}
