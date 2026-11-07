package com.eldercare.backend.care.internal.rules.service;

import com.eldercare.backend.administration.correction.api.events.CorrectionMade;
import com.eldercare.backend.administration.parameter.api.*;
import com.eldercare.backend.administration.parameter.api.dto.ParameterCode;
import com.eldercare.backend.care.api.*;
import com.eldercare.backend.care.api.dto.*;
import com.eldercare.backend.care.api.events.*;
import com.eldercare.backend.care.api.spi.TaskSourcePort.SourceType;
import com.eldercare.backend.care.internal.careplan.service.CarePlanQueries;
import com.eldercare.backend.care.internal.catalog.domain.CareCatalogKinds;
import com.eldercare.backend.care.internal.catalog.persistence.CareCatalogStore;
import com.eldercare.backend.care.internal.result.persistence.CareResultStore;
import com.eldercare.backend.care.internal.rules.domain.*;
import com.eldercare.backend.care.internal.status.persistence.CareLockStore;
import com.eldercare.backend.common.data.ReminderMarkers;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.notification.*;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.resident.api.ResidentStatusApi;
import com.eldercare.backend.resident.api.dto.*;
import com.eldercare.backend.resident.api.spi.ContractApi;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

/** T049: chỉ cảnh báo từ kết quả hiện hành, không tạo công việc. */
@Service
@Transactional(isolation = Isolation.READ_COMMITTED)
public class CareResultRules {
    private final CurrentCareResults current;
    private final CareResultStore results;
    private final CareLockStore locks;
    private final CarePlanQueries plans;
    private final ParameterApi parameters;
    private final ReminderMarkers markers;
    private final CareCatalogStore catalogs;
    private final ResidentStatusApi statuses;
    private final ObjectProvider<ContractApi> contracts;
    private final DayCareAttendanceApi attendance;
    private final ApplicationEventPublisher events;
    private final BusinessTime time;

    public CareResultRules(
            CurrentCareResults current,
            CareResultStore results,
            CareLockStore locks,
            CarePlanQueries plans,
            ParameterApi parameters,
            ReminderMarkers markers,
            CareCatalogStore catalogs,
            ResidentStatusApi statuses,
            ObjectProvider<ContractApi> contracts,
            DayCareAttendanceApi attendance,
            ApplicationEventPublisher events,
            BusinessTime time) {
        this.current = current;
        this.results = results;
        this.locks = locks;
        this.plans = plans;
        this.parameters = parameters;
        this.markers = markers;
        this.catalogs = catalogs;
        this.statuses = statuses;
        this.contracts = contracts;
        this.attendance = attendance;
        this.events = events;
        this.time = time;
    }

    public record WaterSettings(String checkTime, BigDecimal minPercentOfTarget) {}

    public void onResult(long id) {
        var original = results.find(id).orElseThrow();
        if (original.residentId() == null
                || !Set.of("EATING", "MOOD", "BEHAVIOR").contains(original.resultType())) return;
        long resident = original.residentId();
        locks.lock(List.of(resident));
        var rows = current.read(resident, null, null);
        if (original.resultType().equals("EATING")) eating(resident, rows);
        else
            mood(
                    resident,
                    rows,
                    original.performedAt().atZone(BusinessTime.ZONE).toLocalDate());
    }

    @EventListener
    @Transactional(propagation = Propagation.MANDATORY)
    public void onCorrection(CorrectionMade event) {
        if (event.recordType().equals("M04.CARE_RESULT")) onResult(event.originalRecordId());
    }

    private void eating(long resident, List<CurrentCareResults.Value> rows) {
        var meals = rows.stream()
                .filter(v -> v.original().resultType().equals("EATING"))
                .toList();
        var codes = new ArrayList<String>();
        for (var meal : meals) {
            String value = code(meal.values());
            if (!Set.of("ATE_ALL", "MOST", "SOME", "NOT_EATEN", "SKIPPED").contains(value))
                throw unavailable("Mã kết quả ăn hiện hành không hợp lệ");
            codes.add(value);
            if (Set.of("ATE_ALL", "MOST").contains(value)) break;
        }
        int count = PoorMealStreak.count(codes);
        if (count < parameters.read(ParameterCode.CFG_M04_04).asInt()) return;
        var chain = meals.subList(0, count);
        var first = chain.getFirst();
        String marker = chain.stream()
                .map(v -> Long.toString(v.original().id()))
                .collect(java.util.stream.Collectors.joining(","));
        // Dấu dựa vào thành phần chuỗi; không đưa danh sách dài vào khóa hoặc event.
        marker = java.util.HexFormat.of().formatHex(digest(marker));
        if (!markers.mark("M04.POOR_EATING", "M01.RESIDENT", resident, marker, null)) return;
        var context = context(
                first.task().id(),
                "POOR_EATING:" + marker,
                time.now(),
                resident,
                Optional.of(first.task().floorAreaId()));
        events.publishEvent(new AlertRequested(
                "PROLONGED_POOR_EATING",
                NotificationLevel.LOW,
                resident,
                "M04",
                first.task().basisType(),
                first.task().basisId() == null
                        ? first.task().id()
                        : first.task().basisId(),
                context));
        events.publishEvent(new ProlongedPoorEating(
                resident, chain.getLast().task().plannedAt(), first.task().plannedAt(), count, time.now(), context));
    }

    private byte[] digest(String text) {
        try {
            return java.security.MessageDigest.getInstance("SHA-256")
                    .digest(text.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private String code(Map<?, ?> values) {
        if (!(values.get("codeValue") instanceof String code)) throw unavailable("Kết quả hiện hành thiếu mã giá trị");
        return code;
    }

    private void mood(long resident, List<CurrentCareResults.Value> rows, LocalDate end) {
        var moodValues = catalogs.list(CareCatalogKinds.RESULT_VALUES).stream()
                .map(v -> v.attributes())
                .filter(v -> v.type().equals("MOOD"))
                .toList();
        var known = moodValues.stream().map(v -> v.code()).collect(java.util.stream.Collectors.toSet());
        var negative = moodValues.stream()
                .filter(v -> "NEGATIVE".equals(v.ruleGroup()))
                .map(v -> v.code())
                .collect(java.util.stream.Collectors.toSet());
        var days = new HashSet<LocalDate>();
        CurrentCareResults.Value latest = null;
        for (var row : rows) {
            boolean abnormal = false;
            if (row.original().resultType().equals("MOOD")) {
                String value = code(row.values());
                if (!known.contains(value)) throw unavailable("Mã tâm trạng hiện hành không có trong danh mục");
                abnormal = negative.contains(value);
            }
            if (row.original().resultType().equals("BEHAVIOR")) {
                if (!(row.values().get("jsonValue") instanceof Map<?, ?> json)
                        || !(json.get("occurred") instanceof Boolean))
                    throw unavailable("Kết quả hành vi hiện hành không hợp lệ");
                abnormal = Boolean.TRUE.equals(json.get("occurred"));
            }
            var day = row.original().performedAt().atZone(BusinessTime.ZONE).toLocalDate();
            if (abnormal && !day.isAfter(end)) {
                days.add(day);
                if (latest == null) latest = row;
            }
        }
        int count = NegativeMoodStreak.count(days, end);
        if (count < parameters.read(ParameterCode.CFG_M04_05).asInt()) return;
        if (!markers.mark("M04.NEGATIVE_MOOD", "M01.RESIDENT", resident, end + ":" + count, null)) return;
        var task = Objects.requireNonNull(latest).task();
        events.publishEvent(new AlertRequested(
                "PROLONGED_NEGATIVE_MOOD",
                NotificationLevel.LOW,
                resident,
                "M04",
                task.basisType(),
                task.basisId() == null ? task.id() : task.basisId(),
                context(
                        task.id(),
                        "NEGATIVE_MOOD:" + end + ":" + count,
                        time.now(),
                        resident,
                        Optional.of(task.floorAreaId()))));
    }

    public void water(long resident, LocalDate day, Instant now) {
        var setting = parameters.read(ParameterCode.CFG_M04_03).asTable(WaterSettings.class);
        var milestone = day.atTime(LocalTime.parse(setting.checkTime()))
                .atZone(BusinessTime.ZONE)
                .toInstant();
        if (!day.equals(now.atZone(BusinessTime.ZONE).toLocalDate()) || now.isBefore(milestone)) return;
        locks.lock(List.of(resident));
        if (markers.isMarked("M04.WATER_CHECK", "M01.RESIDENT", resident, day.toString())) return;
        var plan = plans.effectiveOn(resident, day);
        if (plan.isEmpty()) return;
        var item = plan.get().items().stream()
                .filter(v -> v.waterTargetMl().isPresent())
                .findFirst();
        if (item.isEmpty()) return;
        var start = day.atStartOfDay(BusinessTime.ZONE).toInstant();
        var source = contracts.getIfAvailable();
        if (source == null) throw unavailable("Chưa có nguồn loại lưu trú");
        var residence = source.currentResidenceType(resident, day)
                .orElseThrow(() -> unavailable("Chưa có loại lưu trú tại ngày kiểm tra"));
        var periods = new ArrayList<StatusPeriod>();
        if (residence == ResidenceType.DAY_CARE) {
            attendance
                    .onDate(resident, day)
                    .filter(v -> v.arrivedAt().isPresent())
                    .ifPresent(v -> periods.add(new StatusPeriod(
                            ResidentStatus.IN_RESIDENCE, v.arrivedAt().orElseThrow(), v.leftAt())));
        } else
            periods.addAll(statuses.statusPeriods(
                    resident, Set.of(ResidentStatus.IN_RESIDENCE), start, milestone.plusNanos(1000)));
        if (periods.stream()
                .noneMatch(v -> !v.from().isAfter(milestone)
                        && v.to().map(t -> t.isAfter(milestone)).orElse(true))) return;
        periods.sort(Comparator.comparing(StatusPeriod::from));
        Instant covered = start;
        long seconds = 0;
        for (var period : periods) {
            var from = period.from().isBefore(covered) ? covered : period.from();
            var to = period.to().filter(t -> t.isBefore(milestone)).orElse(milestone);
            if (to.isAfter(from)) {
                seconds += Duration.between(from, to).getSeconds();
                covered = to;
            }
        }
        long elapsed = Duration.between(start, milestone).toMinutes();
        if (elapsed <= 0) throw unavailable("Mốc kiểm tra nước chưa có khoảng thời gian để tính mục tiêu");
        int target = WaterIntakeRule.adjustedTarget(item.get().waterTargetMl().orElseThrow(), seconds / 60, elapsed);
        BigDecimal total = BigDecimal.ZERO;
        for (var row : current.read(resident, start, milestone.plusNanos(1000))) {
            if (!row.original().resultType().equals("ML")) continue;
            if (!(row.values().get("numericValue") instanceof Number number))
                throw unavailable("Kết quả nước hiện hành không hợp lệ");
            var amount = new BigDecimal(number.toString());
            if (amount.signum() <= 0 || amount.stripTrailingZeros().scale() > 0)
                throw unavailable("Kết quả nước hiện hành phải là số nguyên dương ml");
            total = total.add(amount);
        }
        if (!markers.mark("M04.WATER_CHECK", "M01.RESIDENT", resident, day.toString(), null)) return;
        if (WaterIntakeRule.isLow(total, target, setting.minPercentOfTarget()))
            events.publishEvent(new AlertRequested(
                    "LOW_WATER_INTAKE",
                    NotificationLevel.LOW,
                    resident,
                    "M04",
                    SourceType.CARE_PLAN_ITEM,
                    item.get().id(),
                    context(item.get().id(), "WATER_CHECK:" + day, milestone, resident, Optional.empty())));
    }

    private CareEventContext context(long id, String marker, Instant at, long resident, Optional<Long> floor) {
        return new CareEventContext(
                "M04.CARE_RESULT",
                id,
                marker,
                at,
                floor,
                List.of(new RecipientGroup.AssignedNurse(resident, new Substitution.Default())));
    }

    private BusinessException unavailable(String message) {
        return new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "CARE_SOURCE_UNAVAILABLE", message);
    }
}
