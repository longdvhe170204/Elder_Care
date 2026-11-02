package com.eldercare.backend.staffing.ratio.internal.service;

import com.eldercare.backend.administration.account.api.AccountApi;
import com.eldercare.backend.administration.parameter.api.*;
import com.eldercare.backend.administration.parameter.api.dto.*;
import com.eldercare.backend.admission.api.ContractReadApi;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.resident.api.*;
import com.eldercare.backend.resident.api.dto.*;
import com.eldercare.backend.roombedallocation.api.*;
import com.eldercare.backend.roombedallocation.api.dto.*;
import com.eldercare.backend.staffing.ratio.internal.domain.StaffingRatioCalculator;
import com.eldercare.backend.staffing.shift.internal.domain.WorkShift;
import com.eldercare.backend.staffing.shift.internal.persistence.ShiftStore;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

/** FR-034: chỉ đọc API nguồn; dữ liệu hoặc cấu hình thiếu không trở thành kết quả không người/không trọng số. */
@Service
public class StaffingRatioSources {
    public static class Unavailable extends RuntimeException {
        Unavailable(String message) {
            super(message);
        }

        Unavailable(RuntimeException cause) {
            super("Chưa đọc đủ nguồn tính tỷ lệ phục vụ", cause);
        }
    }

    private static final Set<ResidentStatus> ABSENT =
            Set.of(ResidentStatus.TEMPORARILY_ABSENT, ResidentStatus.HOSPITALIZED, ResidentStatus.OFF_SITE_ACTIVITY);
    private final ObjectProvider<LocationHistoryApi> history;
    private final ObjectProvider<LocationStructureApi> structures;
    private final ObjectProvider<ResidentApi> residents;
    private final ObjectProvider<ResidentStatusApi> statuses;
    private final ObjectProvider<CareLevelApi> careLevels;
    private final ObjectProvider<ContractReadApi> contracts;
    private final AccountApi accounts;
    private final ShiftStore shifts;
    private final ParameterApi parameters;
    private final BusinessTime time;

    public StaffingRatioSources(
            ObjectProvider<LocationHistoryApi> history,
            ObjectProvider<LocationStructureApi> structures,
            ObjectProvider<ResidentApi> residents,
            ObjectProvider<ResidentStatusApi> statuses,
            ObjectProvider<CareLevelApi> careLevels,
            ObjectProvider<ContractReadApi> contracts,
            AccountApi accounts,
            ShiftStore shifts,
            ParameterApi parameters,
            BusinessTime time) {
        this.history = history;
        this.structures = structures;
        this.residents = residents;
        this.statuses = statuses;
        this.careLevels = careLevels;
        this.contracts = contracts;
        this.accounts = accounts;
        this.shifts = shifts;
        this.parameters = parameters;
        this.time = time;
    }

    private <T> T required(ObjectProvider<T> provider) {
        var api = provider.getIfAvailable();
        if (api == null) throw new Unavailable("Chưa có API nguồn tính tỷ lệ phục vụ");
        return api;
    }

    public StaffingRatioCalculator.Result read(WorkShift shift, boolean lockMembers) {
        try {
            var limits = parameters.read(ParameterCode.CFG_M09_01);
            var weightsValue = parameters.read(ParameterCode.CFG_M09_12);
            if (!limits.isSet() || !weightsValue.isSet())
                throw new Unavailable("Chưa cấu hình ngưỡng theo mẫu ca hoặc trọng số chăm sóc");
            var limit = limits.raw().get(Long.toString(shift.shiftTemplateId()));
            if (limit == null || !limit.isNumber()) throw new Unavailable("Chưa cấu hình ngưỡng của mẫu ca");
            var weights = new EnumMap<CareLevel, BigDecimal>(CareLevel.class);
            for (var level : CareLevel.values()) {
                var value = weightsValue.raw().get(level.name());
                if (value == null || !value.isNumber()) throw new Unavailable("Chưa cấu hình đủ ba trọng số chăm sóc");
                weights.put(level, value.decimalValue());
            }
            Instant now = time.now();
            Instant memberAt = shift.startsAt().isAfter(now) ? shift.startsAt() : now;
            if (!memberAt.isBefore(shift.endsAt()))
                throw new Unavailable("Ca đã qua giờ kết thúc, không tính lại bằng dữ liệu hiện hành");
            // FR-034: có mặt/vắng một phần vẫn tính đủ trọng số của cả ca; trạng thái cuối là ngoại lệ FR-035.
            Instant from = shift.startsAt();
            var ids = new TreeSet<Long>();
            var position = required(history);
            var people = required(residents);
            var status = required(statuses);
            var levels = required(careLevels);
            var ref = new LocationRef(shift.scopeType(), shift.scopeId());
            for (LocalDate day = day(from); !day.isAfter(day(shift.endsAt().minusNanos(1))); day = day.plusDays(1)) {
                for (var stay : position.byLocationOn(ref, day))
                    if (stay.from().isBefore(shift.endsAt())
                            && (stay.to() == null || stay.to().isAfter(from))
                            && !endedAt(people, stay.residentId(), memberAt)
                            && !fullyAbsent(
                                    status.statusPeriods(stay.residentId(), ABSENT, from, shift.endsAt()),
                                    from,
                                    shift.endsAt())) ids.add(stay.residentId());
            }
            var attendance = required(contracts);
            var structure = required(structures);
            var daycare = structure.dayCareRestAreaLocation();
            Set<ResidentStatus> candidates = EnumSet.allOf(ResidentStatus.class);
            candidates.removeAll(Set.of(
                    ResidentStatus.RESIDENCY_ENDED, ResidentStatus.DECEASED, ResidentStatus.ADMISSION_CANCELLED));
            for (long resident : people.byStatus(candidates)) {
                for (LocalDate day = day(from);
                        !day.isAfter(day(shift.endsAt().minusNanos(1)));
                        day = day.plusDays(1)) {
                    for (var visit : attendance.attendanceOn(resident, day)) {
                        Instant arrival = visit.date()
                                .atTime(visit.arrivalTime())
                                .atZone(BusinessTime.ZONE)
                                .toInstant();
                        Instant departure = visit.date()
                                .atTime(visit.departureTime())
                                .atZone(BusinessTime.ZONE)
                                .toInstant();
                        if (!arrival.isBefore(departure))
                            throw new Unavailable("Lịch đến bán trú có khoảng giờ không hợp lệ");
                        if (arrival.isBefore(shift.endsAt()) && departure.isAfter(from)) {
                            if (daycare.isEmpty()) throw new Unavailable("Chưa xác định vị trí khu nghỉ bán trú");
                            if (matches(shift, daycare.get())) ids.add(resident);
                        }
                    }
                }
            }
            var expected = new ArrayList<StaffingRatioCalculator.Resident>();
            for (long id : ids)
                expected.add(new StaffingRatioCalculator.Resident(
                        id,
                        levels.current(id)
                                .orElseThrow(
                                        () -> new Unavailable("Người dự kiến có mặt chưa có mức chăm sóc hiện hành"))));
            var members = shifts.members(shift.id(), lockMembers).stream()
                    .map(m -> new StaffingRatioCalculator.Member(
                            m.staffId(),
                            accounts.byStaff(m.staffId()).map(a -> a.roles()).orElse(Set.of()),
                            !m.presentAt(memberAt),
                            m.approvedLeaveRequestId() != null))
                    .toList();
            return StaffingRatioCalculator.calculate(expected, members, weights, limit.decimalValue());
        } catch (Unavailable e) {
            throw e;
        } catch (RuntimeException e) {
            throw new Unavailable(e);
        }
    }

    private boolean endedAt(ResidentApi api, long resident, Instant from) {
        return api.isFinalStatus(resident)
                && api.finalStatusAt(resident).map(at -> !at.isAfter(from)).orElse(true);
    }

    private static LocalDate day(Instant at) {
        return LocalDate.ofInstant(at, BusinessTime.ZONE);
    }

    static boolean fullyAbsent(List<StatusPeriod> periods, Instant from, Instant to) {
        Instant cursor = from;
        for (var period : periods.stream()
                .sorted(Comparator.comparing(StatusPeriod::from))
                .toList()) {
            if (period.from().isAfter(cursor)) return false;
            Instant end = period.to().orElse(to);
            if (end.isAfter(cursor)) cursor = end;
            if (!cursor.isBefore(to)) return true;
        }
        return false;
    }

    public boolean affects(WorkShift shift, long resident, Instant effectiveAt) {
        for (var stay : required(history).byResident(resident, effectiveAt.minusNanos(1000), shift.endsAt())) {
            var p = stay.location();
            if (p.floorId() != null && matches(shift, new LocationRef("FLOOR", p.floorId()))) return true;
            if (p.areaId() != null && matches(shift, new LocationRef("AREA", p.areaId()))) return true;
        }
        var rest = required(structures).dayCareRestAreaLocation();
        if (rest.isEmpty() || !matches(shift, rest.get())) return false;
        Instant begin = shift.startsAt().isAfter(effectiveAt) ? shift.startsAt() : effectiveAt;
        for (LocalDate date = day(begin); !date.isAfter(day(shift.endsAt().minusNanos(1))); date = date.plusDays(1))
            if (!required(contracts).attendanceOn(resident, date).isEmpty()) return true;
        return false;
    }

    public boolean matches(WorkShift shift, LocationRef ref) {
        if (shift.scopeType().equals(ref.type())) return Objects.equals(shift.scopeId(), ref.id());
        var api = required(structures);
        Long floor = switch (ref.type()) {
            case "FLOOR" -> ref.id();
            case "ROOM" -> api.room(ref.id()).map(RoomView::floorId).orElse(null);
            default -> null;
        };
        if (floor == null) return false;
        if ("FLOOR".equals(shift.scopeType())) return Objects.equals(shift.scopeId(), floor);
        return api.floor(floor)
                .map(f -> Objects.equals(f.areaId(), shift.scopeId()))
                .orElse(false);
    }
}
