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

    /** Dòng lịch sử trạng thái, mức chăm sóc của M01 ở {@code status_history} (001 T084). */
    private static final String STATUS = "entity_type = 'M01.RESIDENT' AND attribute = 'STATUS'";

    private static final String CARE_LEVEL = "entity_type = 'M01.RESIDENT' AND attribute = 'CARE_LEVEL'";

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
                "SELECT entity_id AS resident_id, starts_at AS at, command, value_before, value,"
                        + " ROW_NUMBER() OVER (PARTITION BY entity_id ORDER BY starts_at DESC, id DESC) AS rn"
                        + " FROM status_history WHERE " + STATUS + " AND starts_at < :before",
                before,
                residentIds);
    }

    /** Dòng mức chăm sóc hiệu lực mới nhất trước {@code before} của mỗi người. */
    public List<HistoryPoint> latestCareLevelBefore(Instant before, Collection<Long> residentIds) {
        return latest(
                "SELECT entity_id AS resident_id, starts_at AS at, NULL AS command, NULL AS value_before, value,"
                        + " ROW_NUMBER() OVER (PARTITION BY entity_id ORDER BY starts_at DESC, id DESC) AS rn"
                        + " FROM status_history WHERE " + CARE_LEVEL + " AND starts_at < :before",
                before,
                residentIds);
    }

    /** Lệnh trạng thái trong {@code [from, to)}. */
    public List<HistoryPoint> statusChanges(Instant from, Instant to, Collection<Long> residentIds) {
        return between(
                "SELECT entity_id AS resident_id, starts_at AS at, command, value_before, value FROM status_history"
                        + " WHERE " + STATUS + " AND starts_at >= :from AND starts_at < :to",
                from,
                to,
                residentIds);
    }

    /** Đổi mức chăm sóc theo thời điểm hiệu lực trong {@code [from, to)}. */
    public List<HistoryPoint> careLevelChanges(Instant from, Instant to, Collection<Long> residentIds) {
        return between(
                "SELECT entity_id AS resident_id, starts_at AS at, NULL AS command, NULL AS value_before, value"
                        + " FROM status_history WHERE " + CARE_LEVEL + " AND starts_at >= :from AND starts_at < :to",
                from,
                to,
                residentIds);
    }

    private List<HistoryPoint> latest(String inner, Instant before, Collection<Long> residentIds) {
        MapSqlParameterSource params = new MapSqlParameterSource("before", Timestamp.from(before));
        return jdbc.query("SELECT * FROM (" + inner + scope(residentIds, params) + ") x WHERE rn = 1", params, ROW);
    }

    private List<HistoryPoint> between(String sql, Instant from, Instant to, Collection<Long> residentIds) {
        MapSqlParameterSource params =
                new MapSqlParameterSource(Map.of("from", Timestamp.from(from), "to", Timestamp.from(to)));
        return jdbc.query(sql + scope(residentIds, params) + " ORDER BY starts_at, id", params, ROW);
    }

    private static String scope(Collection<Long> residentIds, MapSqlParameterSource params) {
        if (residentIds == null) {
            return "";
        }
        params.addValue("ids", residentIds);
        return " AND entity_id IN (:ids)";
    }
}
