package com.eldercare.backend.care.internal.attendance.service;

import com.eldercare.backend.admission.api.ContractReadApi;
import com.eldercare.backend.care.api.spi.DayCareCalendarPort;
import com.eldercare.backend.care.internal.attendance.domain.AttendanceDaySource;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.resident.api.ResidentApi;
import com.eldercare.backend.resident.api.dto.*;
import com.eldercare.backend.resident.api.spi.ContractApi;
import com.eldercare.backend.roombedallocation.api.LocationStructureApi;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class AttendanceSources {
    private final ObjectProvider<ContractReadApi> schedules;
    private final ObjectProvider<ContractApi> contracts;
    private final ObjectProvider<DayCareCalendarPort> calendar;
    private final LocationStructureApi locations;
    private final ResidentApi residents;

    public AttendanceSources(
            ObjectProvider<ContractReadApi> schedules,
            ObjectProvider<ContractApi> contracts,
            ObjectProvider<DayCareCalendarPort> calendar,
            LocationStructureApi locations,
            ResidentApi residents) {
        this.schedules = schedules;
        this.contracts = contracts;
        this.calendar = calendar;
        this.locations = locations;
        this.residents = residents;
    }

    public static BusinessException unavailable(String detail) {
        return new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "CARE_SOURCE_UNAVAILABLE", detail);
    }

    public boolean dayCare(long resident, LocalDate date) {
        var p = contracts.getIfAvailable();
        if (p == null) throw unavailable("Chưa có nguồn loại lưu trú");
        return p.currentResidenceType(resident, date)
                .filter(r -> r == ResidenceType.DAY_CARE)
                .isPresent();
    }

    public boolean staying(long resident, LocalDate date) {
        return residents.status(resident) == ResidentStatus.IN_RESIDENCE && dayCare(resident, date);
    }

    public void checkResident(long resident, LocalDate date) {
        if (!staying(resident, date))
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "DAY_CARE_NOT_STAYING",
                    "Người cao tuổi phải Đang lưu trú và có hợp đồng bán trú");
    }

    public AttendanceDaySource day(long resident, LocalDate date) {
        var s = schedules.getIfAvailable();
        var c = calendar.getIfAvailable();
        if (s == null || c == null) throw unavailable("Chưa có nguồn lịch đến/ngày nghỉ/báo vắng 004");
        var days = s.attendanceOn(resident, date);
        if (days.size() > 1) throw unavailable("Nguồn004 trả nhiều khung trong một ngày; cần xác nhận khung theo ngày");
        var policy = c.onDate(resident, date);
        if (policy == null) throw unavailable("Nguồn004 chưa xác định ngày nghỉ/báo vắng");
        return new AttendanceDaySource(
                days.isEmpty() || policy.holiday() ? null : days.getFirst().arrivalTime(),
                days.isEmpty() || policy.holiday() ? null : days.getFirst().departureTime(),
                policy);
    }

    public long floor() {
        var ref =
                locations.dayCareRestAreaLocation().orElseThrow(() -> unavailable("Chưa xác định vị trí khu bán trú"));
        if (!locations.active(ref)) throw unavailable("Vị trí khu bán trú chưa có hiệu lực");
        return switch (ref.type()) {
            case "FLOOR", "AREA" -> ref.id();
            case "ROOM" ->
                locations
                        .room(ref.id())
                        .orElseThrow(() -> unavailable("Chưa xác định tầng của khu bán trú"))
                        .floorId();
            default -> throw unavailable("Loại vị trí khu bán trú chưa hỗ trợ");
        };
    }

    public List<Long> currentResidents(LocalDate date) {
        return residents.byStatus(Set.of(ResidentStatus.IN_RESIDENCE)).stream()
                .filter(id -> dayCare(id, date))
                .sorted()
                .toList();
    }

    public List<LocalDate> dueDates(LocalDate today) {
        var ids = residents.byStatus(Set.of(ResidentStatus.IN_RESIDENCE));
        if (ids.isEmpty()) return List.of();
        var p = contracts.getIfAvailable();
        if (p == null) throw unavailable("Chưa có lịch sử loại lưu trú004 để xác định mốc chạy bù");
        var dates = new TreeSet<LocalDate>();
        for (long id : ids)
            for (var period : p.residenceTypeHistory(id)) {
                if (period.type() != ResidenceType.DAY_CARE || period.from().isAfter(today)) continue;
                var end = period.to().filter(d -> d.isBefore(today)).orElse(today);
                if (!end.isBefore(period.from()))
                    period.from().datesUntil(end.plusDays(1)).forEach(dates::add);
            }
        return List.copyOf(dates);
    }
}
