package com.eldercare.backend.staffing.internal.assignment.service;

import com.eldercare.backend.admission.api.ContractReadApi;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.resident.api.*;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import com.eldercare.backend.roombedallocation.api.dto.LocationRef;
import com.eldercare.backend.staffing.internal.assignment.domain.PrimaryAssigneeResolver.TargetRef;
import com.eldercare.backend.staffing.internal.shift.domain.WorkShift;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/** Nguồn đọc theo mốc của T055, không dùng trạng thái hôm nay thay lịch sử. */
@Service
public class AssignmentReadSources {
    private final AssignmentSources sources;
    private final ObjectProvider<ResidentApi> residents;
    private final ObjectProvider<ResidentStatusApi> statuses;
    private final ObjectProvider<ContractReadApi> contracts;
    private static final Set<ResidentStatus> ABSENT =
            Set.of(ResidentStatus.TEMPORARILY_ABSENT, ResidentStatus.HOSPITALIZED, ResidentStatus.OFF_SITE_ACTIVITY);

    public AssignmentReadSources(
            AssignmentSources sources,
            ObjectProvider<ResidentApi> residents,
            ObjectProvider<ResidentStatusApi> statuses,
            ObjectProvider<ContractReadApi> contracts) {
        this.sources = sources;
        this.residents = residents;
        this.statuses = statuses;
        this.contracts = contracts;
    }

    public Set<TargetRef> position(long resident, Instant at) {
        try {
            AssignmentSources.required(residents).identity(resident);
            var refs = new HashSet<TargetRef>();
            refs.add(new TargetRef("RESIDENT", resident));
            var p = sources.position(resident, at);
            if (p.isPresent()) {
                if (p.get().roomId() != null)
                    refs.add(new TargetRef("ROOM", p.get().roomId()));
                if (p.get().floorId() != null)
                    refs.add(new TargetRef("FLOOR", p.get().floorId()));
                if (p.get().areaId() != null)
                    refs.add(new TargetRef("AREA", p.get().areaId()));
            } else {
                var visit = visiting(resident, at);
                if (!visit) throw unavailable();
                add(
                        refs,
                        sources.structure().dayCareRestAreaLocation().orElseThrow(AssignmentReadSources::unavailable));
            }
            return Set.copyOf(refs);
        } catch (RuntimeException e) {
            throw unavailable();
        }
    }

    public Map<Long, Set<TargetRef>> expected(WorkShift shift) {
        try {
            Instant at = shift.startsAt();
            var people = AssignmentSources.required(residents);
            var ids = new TreeSet<>(
                    sources.residents(shift.scopeType(), shift.scopeId() == null ? 0 : shift.scopeId(), at));
            // Bao gồm cả hồ sơ nay đã kết thúc để truy vấn quá khứ không bỏ người bán trú.
            for (long resident : people.byStatus(EnumSet.allOf(ResidentStatus.class)))
                if (visiting(resident, at)) {
                    var refs = position(resident, at);
                    if (inShift(shift, refs)) ids.add(resident);
                }
            var result = new TreeMap<Long, Set<TargetRef>>();
            var status = AssignmentSources.required(statuses);
            for (long resident : ids) {
                if (people.isFinalStatus(resident)
                        && !people.finalStatusAt(resident)
                                .orElseThrow(AssignmentReadSources::unavailable)
                                .isAfter(at)) continue;
                if (status.statusPeriods(resident, ABSENT, at, at.plusNanos(1000)).stream()
                        .anyMatch(p -> !p.from().isAfter(at)
                                && p.to().map(end -> end.isAfter(at)).orElse(true))) continue;
                var refs = position(resident, at);
                if (!inShift(shift, refs)) throw unavailable();
                result.put(resident, refs);
            }
            return result;
        } catch (RuntimeException e) {
            throw unavailable();
        }
    }

    private boolean visiting(long resident, Instant at) {
        LocalDate day = LocalDate.ofInstant(at, BusinessTime.ZONE);
        boolean present = false;
        for (var v : AssignmentSources.required(contracts).attendanceOn(resident, day)) {
            Instant arrival =
                    v.date().atTime(v.arrivalTime()).atZone(BusinessTime.ZONE).toInstant();
            Instant departure =
                    v.date().atTime(v.departureTime()).atZone(BusinessTime.ZONE).toInstant();
            if (!arrival.isBefore(departure)) throw unavailable();
            if (!at.isBefore(arrival) && at.isBefore(departure)) present = true;
        }
        return present;
    }

    private void add(Set<TargetRef> refs, LocationRef ref) {
        refs.add(new TargetRef(ref.type(), ref.id()));
        Long floor = switch (ref.type()) {
            case "ROOM" ->
                sources.structure()
                        .room(ref.id())
                        .orElseThrow(AssignmentReadSources::unavailable)
                        .floorId();
            case "FLOOR" -> ref.id();
            case "AREA" -> null;
            default -> throw unavailable();
        };
        if (floor != null) {
            refs.add(new TargetRef("FLOOR", floor));
            var f = sources.structure().floor(floor).orElseThrow(AssignmentReadSources::unavailable);
            refs.add(new TargetRef("AREA", f.areaId()));
        }
    }

    public static boolean inShift(WorkShift shift, Set<TargetRef> refs) {
        return "FACILITY".equals(shift.scopeType()) || refs.contains(new TargetRef(shift.scopeType(), shift.scopeId()));
    }

    public static BusinessException unavailable() {
        return new BusinessException(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "ASSIGNMENT_SOURCE_UNAVAILABLE",
                "Không xác định được kết quả phân công từ dữ liệu nguồn");
    }
}
