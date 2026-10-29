package com.eldercare.backend.staffing.ratio.internal.persistence;

import com.eldercare.backend.staffing.ratio.internal.domain.*;
import com.eldercare.backend.staffing.shift.internal.persistence.ShiftStore;
import java.sql.*;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.*;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class StaffingRatioStore {
    private final JdbcTemplate jdbc;

    public StaffingRatioStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final RowMapper<StaffingRatioCalculation> ROW = (r, i) -> new StaffingRatioCalculation(
            r.getLong("id"),
            r.getLong("shift_id"),
            r.getObject("calculated_at", LocalDateTime.class).toInstant(ZoneOffset.UTC),
            r.getString("event"),
            r.getBigDecimal("total_weight"),
            r.getInt("staff_count"),
            r.getBigDecimal("ratio"),
            r.getBigDecimal("threshold"),
            r.getBoolean("meets_threshold"));

    public List<StaffingRatioCalculation> history(long shift) {
        return jdbc.query(
                "SELECT * FROM staffing_ratio_calculation WHERE shift_id=? ORDER BY calculated_at,id", ROW, shift);
    }

    public Optional<StaffingRatioCalculation> latest(long shift) {
        return jdbc
                .query(
                        "SELECT * FROM staffing_ratio_calculation WHERE shift_id=? ORDER BY calculated_at DESC,id DESC LIMIT 1",
                        ROW,
                        shift)
                .stream()
                .findFirst();
    }

    public List<StaffingRatioCalculation> forReport(Instant from, Instant to) {
        return jdbc.query(
                "SELECT c.* FROM staffing_ratio_calculation c JOIN work_shift s ON s.id=c.shift_id WHERE s.status NOT IN ('DRAFT','CANCELLED') AND s.starts_at<? AND s.ends_at>? AND c.calculated_at<s.ends_at AND NOT EXISTS(SELECT 1 FROM staffing_ratio_calculation n WHERE n.shift_id=c.shift_id AND n.calculated_at<s.ends_at AND (n.calculated_at>c.calculated_at OR (n.calculated_at=c.calculated_at AND n.id>c.id))) ORDER BY s.starts_at,s.id",
                ROW,
                ShiftStore.sql(to),
                ShiftStore.sql(from));
    }

    public Set<Long> everBelow(Instant from, Instant to) {
        return new HashSet<>(jdbc.queryForList(
                "SELECT DISTINCT c.shift_id FROM staffing_ratio_calculation c JOIN work_shift s ON s.id=c.shift_id WHERE s.starts_at<? AND s.ends_at>? AND c.calculated_at<s.ends_at AND c.meets_threshold=FALSE",
                Long.class,
                ShiftStore.sql(to),
                ShiftStore.sql(from)));
    }

    public StaffingRatioCalculation append(
            long shift, Instant at, String event, StaffingRatioCalculator.Result result) {
        var key = new GeneratedKeyHolder();
        jdbc.update(
                c -> {
                    var p = c.prepareStatement(
                            "INSERT INTO staffing_ratio_calculation(shift_id,calculated_at,event,total_weight,staff_count,ratio,threshold,meets_threshold) VALUES (?,?,?,?,?,?,?,?)",
                            Statement.RETURN_GENERATED_KEYS);
                    p.setLong(1, shift);
                    p.setObject(2, ShiftStore.sql(at));
                    p.setString(3, event);
                    p.setBigDecimal(4, result.totalWeight());
                    p.setInt(5, result.staffCount());
                    p.setBigDecimal(6, result.ratio());
                    p.setBigDecimal(7, result.threshold());
                    p.setBoolean(8, result.meetsThreshold());
                    return p;
                },
                key);
        return new StaffingRatioCalculation(
                key.getKey().longValue(),
                shift,
                at,
                event,
                result.totalWeight(),
                result.staffCount(),
                result.ratio(),
                result.threshold(),
                result.meetsThreshold());
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
