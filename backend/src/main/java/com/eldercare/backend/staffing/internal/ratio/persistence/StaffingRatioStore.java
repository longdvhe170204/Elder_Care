package com.eldercare.backend.staffing.internal.ratio.persistence;

import com.eldercare.backend.administration.shared.api.RecordEventApi;
import com.eldercare.backend.administration.shared.api.dto.*;
import com.eldercare.backend.staffing.internal.domain.StaffingCatalogData.Legacy;
import com.eldercare.backend.staffing.internal.domain.StaffingEventData;
import com.eldercare.backend.staffing.internal.ratio.domain.*;
import com.eldercare.backend.staffing.internal.shift.persistence.ShiftStore;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** T050: tỷ lệ chỉ thêm vào record_event, mọi trường của bảng cũ giữ trong nội dung. */
@Repository
public class StaffingRatioStore {
    private final JdbcTemplate jdbc;
    private final RecordEventApi events;
    private static final Comparator<StaffingRatioCalculation> ORDER =
            Comparator.comparing(StaffingRatioCalculation::at).thenComparingLong(StaffingRatioCalculation::id);

    public StaffingRatioStore(JdbcTemplate jdbc, RecordEventApi events) {
        this.jdbc = jdbc;
        this.events = events;
    }

    private StaffingRatioCalculation view(RecordEvent<StaffingEventData.Ratio> r) {
        var a = r.content();
        return new StaffingRatioCalculation(
                Legacy.logicalId(r.id(), a.legacy(), "staffing_ratio_calculation"),
                r.parent().entityId(),
                a.calculatedAt(),
                a.event(),
                a.totalWeight(),
                a.staffCount(),
                a.ratio(),
                a.threshold(),
                a.meetsThreshold());
    }

    public List<StaffingRatioCalculation> history(long shift) {
        return events.list(StaffingEventData.RATIO, new EntityRef("M09.SHIFT", shift)).stream()
                .map(this::view)
                .sorted(ORDER)
                .toList();
    }

    public Optional<StaffingRatioCalculation> latest(long shift) {
        return history(shift).stream().max(ORDER);
    }

    public List<StaffingRatioCalculation> forReport(Instant from, Instant to) {
        var shifts = jdbc.query(
                "SELECT id,ends_at FROM work_shift WHERE status NOT IN ('DRAFT','CANCELLED') AND starts_at<? AND ends_at>? ORDER BY starts_at,id",
                (r, i) -> Map.entry(
                        r.getLong("id"),
                        r.getObject("ends_at", LocalDateTime.class).toInstant(ZoneOffset.UTC)),
                ShiftStore.sql(to),
                ShiftStore.sql(from));
        var result = new ArrayList<StaffingRatioCalculation>();
        for (var shift : shifts)
            history(shift.getKey()).stream()
                    .filter(c -> c.at().isBefore(shift.getValue()))
                    .max(ORDER)
                    .ifPresent(result::add);
        return result;
    }

    public Set<Long> everBelow(Instant from, Instant to) {
        var shifts = jdbc.query(
                "SELECT id,ends_at FROM work_shift WHERE starts_at<? AND ends_at>?",
                (r, i) -> Map.entry(
                        r.getLong("id"),
                        r.getObject("ends_at", LocalDateTime.class).toInstant(ZoneOffset.UTC)),
                ShiftStore.sql(to),
                ShiftStore.sql(from));
        var result = new HashSet<Long>();
        for (var shift : shifts)
            if (history(shift.getKey()).stream()
                    .anyMatch(c -> c.at().isBefore(shift.getValue()) && !c.meetsThreshold()))
                result.add(shift.getKey());
        return result;
    }

    public StaffingRatioCalculation append(
            long shift, Instant at, String event, StaffingRatioCalculator.Result result) {
        return view(events.add(
                StaffingEventData.RATIO,
                new EntityRef("M09.SHIFT", shift),
                new NewRecordEvent<>(
                        new StaffingEventData.Ratio(
                                at,
                                event,
                                result.totalWeight(),
                                result.staffCount(),
                                result.ratio(),
                                result.threshold(),
                                result.meetsThreshold(),
                                null),
                        null,
                        null,
                        true,
                        at,
                        null,
                        at,
                        null,
                        null)));
    }

    public boolean below(long shift) {
        return Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT below_threshold FROM work_shift WHERE id=? FOR UPDATE", Boolean.class, shift));
    }

    public boolean managerNotified(long shift) {
        return Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT manager_notified_below_threshold_at IS NOT NULL FROM work_shift WHERE id=? FOR UPDATE",
                Boolean.class,
                shift));
    }

    public void flag(long shift, boolean below) {
        jdbc.update(
                "UPDATE work_shift SET below_threshold=?,manager_notified_below_threshold_at=IF(?,manager_notified_below_threshold_at,NULL) WHERE id=?",
                below,
                below,
                shift);
    }

    public void managerNotified(long shift, Instant at) {
        jdbc.update(
                "UPDATE work_shift SET manager_notified_below_threshold_at=? WHERE id=?", ShiftStore.sql(at), shift);
    }

    public List<Long> upcomingBelow(Instant at, Instant until) {
        return jdbc.queryForList(
                "SELECT id FROM work_shift WHERE scope_type IN ('FLOOR','AREA') AND status IN ('PUBLISHED','IN_PROGRESS') AND below_threshold=TRUE AND manager_notified_below_threshold_at IS NULL AND starts_at<=? AND ends_at>? ORDER BY id",
                Long.class,
                ShiftStore.sql(until),
                ShiftStore.sql(at));
    }

    public List<Long> openShifts(Instant effectiveAt) {
        return jdbc.queryForList(
                "SELECT id FROM work_shift WHERE scope_type IN ('FLOOR','AREA') AND status NOT IN ('CLOSED','CANCELLED') AND ends_at>? ORDER BY id",
                Long.class,
                ShiftStore.sql(effectiveAt));
    }
}
