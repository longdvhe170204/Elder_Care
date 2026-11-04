package com.eldercare.backend.resident.internal.status.persistence;

import com.eldercare.backend.resident.internal.status.domain.HistoryPoint;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Đọc lịch sử trạng thái, mức chăm sóc cho số liệu của 016 (module-interfaces 001 mục 1.8a). Chỉ đọc, không khóa.
 * {@code residentIds} {@code null} là toàn viện.
 */
@Repository
public class ResidentStatisticsStore {

    private static final RowMapper<HistoryPoint> ROW = (rs, i) -> new HistoryPoint(
            rs.getLong("resident_id"),
            rs.getTimestamp("at").toInstant(),
            rs.getString("command"),
            rs.getString("value_before"),
            rs.getString("value"));

    private final NamedParameterJdbcTemplate jdbc;

    ResidentStatisticsStore(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Dòng trạng thái mới nhất trước {@code before} của mỗi người. */
    public List<HistoryPoint> latestStatusBefore(Instant before, Collection<Long> residentIds) {
        return latest(
                "SELECT resident_id, occurred_at AS at, command, from_status AS value_before, to_status AS value,"
                        + " ROW_NUMBER() OVER (PARTITION BY resident_id ORDER BY occurred_at DESC, id DESC) AS rn"
                        + " FROM resident_status_history WHERE occurred_at < :before",
                before,
                residentIds);
    }

    /** Dòng mức chăm sóc hiệu lực mới nhất trước {@code before} của mỗi người. */
    public List<HistoryPoint> latestCareLevelBefore(Instant before, Collection<Long> residentIds) {
        return latest(
                "SELECT resident_id, effective_from AS at, NULL AS command, NULL AS value_before, care_level AS value,"
                        + " ROW_NUMBER() OVER (PARTITION BY resident_id ORDER BY effective_from DESC, id DESC) AS rn"
                        + " FROM care_level_history WHERE effective_from < :before",
                before,
                residentIds);
    }

    /** Lệnh trạng thái trong {@code [from, to)}. */
    public List<HistoryPoint> statusChanges(Instant from, Instant to, Collection<Long> residentIds) {
        return between(
                "SELECT resident_id, occurred_at AS at, command, from_status AS value_before, to_status AS value"
                        + " FROM resident_status_history WHERE occurred_at >= :from AND occurred_at < :to",
                "occurred_at",
                from,
                to,
                residentIds);
    }

    /** Đổi mức chăm sóc theo thời điểm hiệu lực trong {@code [from, to)}. */
    public List<HistoryPoint> careLevelChanges(Instant from, Instant to, Collection<Long> residentIds) {
        return between(
                "SELECT resident_id, effective_from AS at, NULL AS command, NULL AS value_before, care_level AS value"
                        + " FROM care_level_history WHERE effective_from >= :from AND effective_from < :to",
                "effective_from",
                from,
                to,
                residentIds);
    }

    private List<HistoryPoint> latest(String inner, Instant before, Collection<Long> residentIds) {
        MapSqlParameterSource params = new MapSqlParameterSource("before", Timestamp.from(before));
        return jdbc.query("SELECT * FROM (" + inner + scope(residentIds, params) + ") x WHERE rn = 1", params, ROW);
    }

    private List<HistoryPoint> between(
            String sql, String orderColumn, Instant from, Instant to, Collection<Long> residentIds) {
        MapSqlParameterSource params =
                new MapSqlParameterSource(Map.of("from", Timestamp.from(from), "to", Timestamp.from(to)));
        return jdbc.query(sql + scope(residentIds, params) + " ORDER BY " + orderColumn + ", id", params, ROW);
    }

    private static String scope(Collection<Long> residentIds, MapSqlParameterSource params) {
        if (residentIds == null) {
            return "";
        }
        params.addValue("ids", residentIds);
        return " AND resident_id IN (:ids)";
    }
}
