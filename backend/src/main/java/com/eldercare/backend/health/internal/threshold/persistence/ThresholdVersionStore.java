package com.eldercare.backend.health.internal.threshold.persistence;

import com.eldercare.backend.health.internal.threshold.domain.ThresholdBand;
import com.eldercare.backend.health.internal.threshold.domain.ThresholdListing;
import com.eldercare.backend.health.internal.threshold.domain.ThresholdVersion;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/**
 * Phiên bản ngưỡng trong {@code monitoring_setting} (kind THRESHOLD; research S-07): tối đa một Hiệu lực và một Chờ
 * hiệu lực mỗi (phạm vi, chỉ số) bằng cột sinh {@code UNIQUE}; trigger chặn sửa nội dung và mọi đổi từ trạng thái cuối.
 */
@Repository
public class ThresholdVersionStore {
    private static final String COLUMNS =
            "id, scope, resident_id, indicator_id, effective_from, effective_until, status, components, version";

    private final JdbcTemplate jdbc;
    private final JsonMapper json;

    public ThresholdVersionStore(JdbcTemplate jdbc, JsonMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    /** Phiên bản Chờ hiệu lực và Hiệu lực của (phạm vi, người cao tuổi, chỉ số). */
    public List<ThresholdVersion> open(String scope, Long residentId, long indicatorId) {
        return jdbc.query(
                "SELECT " + COLUMNS + " FROM monitoring_setting WHERE kind = 'THRESHOLD' AND scope = ?"
                        + " AND resident_id <=> ? AND indicator_id = ? AND status IN ('PENDING','EFFECTIVE') ORDER BY id",
                this::map,
                scope,
                residentId,
                indicatorId);
    }

    /** Ngưỡng cá nhân Chờ hiệu lực, Hiệu lực của người cao tuổi (mọi chỉ số). */
    public List<ThresholdVersion> personalOpen(long residentId) {
        return jdbc.query(
                "SELECT " + COLUMNS + " FROM monitoring_setting WHERE kind = 'THRESHOLD' AND scope = 'PERSONAL'"
                        + " AND resident_id = ? AND status IN ('PENDING','EFFECTIVE') ORDER BY id",
                this::map,
                residentId);
    }

    public boolean hasPersonalOpen(long residentId) {
        return Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM monitoring_setting WHERE kind = 'THRESHOLD' AND scope = 'PERSONAL'"
                        + " AND resident_id = ? AND status IN ('PENDING','EFFECTIVE'))",
                Boolean.class,
                residentId));
    }

    /** Lịch sử phiên bản theo bộ lọc (trống là không lọc), mới nhất trước. */
    public List<ThresholdListing> list(String scope, Long residentId, Long indicatorId) {
        var sql = new StringBuilder(
                "SELECT " + COLUMNS + ", reason, created_by FROM monitoring_setting WHERE kind = 'THRESHOLD'");
        var args = new java.util.ArrayList<Object>();
        if (scope != null) {
            sql.append(" AND scope = ?");
            args.add(scope);
        }
        if (residentId != null) {
            sql.append(" AND resident_id = ?");
            args.add(residentId);
        }
        if (indicatorId != null) {
            sql.append(" AND indicator_id = ?");
            args.add(indicatorId);
        }
        sql.append(" ORDER BY effective_from DESC, id DESC");
        return jdbc.query(
                sql.toString(),
                (rs, row) -> new ThresholdListing(map(rs, row), rs.getString("reason"), rs.getLong("created_by")),
                args.toArray());
    }

    public long insert(ThresholdVersion version, String reason, long createdBy, Instant createdAt) {
        var keys = new GeneratedKeyHolder();
        jdbc.update(
                connection -> {
                    var ps = connection.prepareStatement("""
                            INSERT INTO monitoring_setting (kind, scope, resident_id, indicator_id, effective_from,
                              status, components, reason, created_by, created_at)
                            VALUES ('THRESHOLD', ?, ?, ?, ?, ?, ?, ?, ?, ?)
                            """, Statement.RETURN_GENERATED_KEYS);
                    ps.setString(1, version.scope());
                    ps.setObject(2, version.residentId());
                    ps.setLong(3, version.indicatorId());
                    ps.setTimestamp(4, Timestamp.from(version.effectiveFrom()));
                    ps.setString(5, version.status());
                    ps.setString(6, json.writeValueAsString(version.components()));
                    ps.setString(7, reason);
                    ps.setLong(8, createdBy);
                    ps.setTimestamp(9, Timestamp.from(createdAt));
                    return ps;
                },
                keys);
        return keys.getKey().longValue();
    }

    /** Kết thúc phiên bản: {@code EXPIRED} (đặt {@code effective_until}) hoặc {@code CANCELLED}. */
    public void end(
            long id, String status, Instant effectiveUntil, String endedReason, Long endedBy, int expectedVersion) {
        int changed = jdbc.update(
                """
                UPDATE monitoring_setting SET status = ?, effective_until = ?, ended_reason = ?, ended_by = ?,
                  version = version + 1
                WHERE id = ? AND version = ? AND status IN ('PENDING','EFFECTIVE')
                """,
                status,
                effectiveUntil == null ? null : Timestamp.from(effectiveUntil),
                endedReason,
                endedBy,
                id,
                expectedVersion);
        if (changed != 1) throw new IllegalStateException("Phiên bản ngưỡng đã thay đổi đồng thời");
    }

    public void activate(long id, int expectedVersion) {
        int changed = jdbc.update(
                "UPDATE monitoring_setting SET status = 'EFFECTIVE', version = version + 1"
                        + " WHERE id = ? AND version = ? AND status = 'PENDING'",
                id,
                expectedVersion);
        if (changed != 1) throw new IllegalStateException("Phiên bản ngưỡng đã thay đổi đồng thời");
    }

    public Optional<ThresholdVersion> lock(long id) {
        return jdbc
                .query(
                        "SELECT " + COLUMNS + " FROM monitoring_setting WHERE id = ? AND kind = 'THRESHOLD' FOR UPDATE",
                        this::map,
                        id)
                .stream()
                .findFirst();
    }

    /** Phiên bản Chờ hiệu lực đã tới thời điểm hiệu lực. */
    public List<ThresholdVersion> due(Instant at) {
        return jdbc.query(
                "SELECT " + COLUMNS + " FROM monitoring_setting WHERE kind = 'THRESHOLD' AND status = 'PENDING'"
                        + " AND effective_from <= ? ORDER BY effective_from, id",
                this::map,
                Timestamp.from(at));
    }

    private ThresholdVersion map(ResultSet rs, int row) throws SQLException {
        var until = rs.getTimestamp("effective_until");
        return new ThresholdVersion(
                rs.getLong("id"),
                rs.getString("scope"),
                (Long) rs.getObject("resident_id"),
                rs.getLong("indicator_id"),
                rs.getTimestamp("effective_from").toInstant(),
                until == null ? null : until.toInstant(),
                rs.getString("status"),
                json.readValue(rs.getString("components"), new TypeReference<Map<String, ThresholdBand>>() {}),
                rs.getInt("version"));
    }
}
