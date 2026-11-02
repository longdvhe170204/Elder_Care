package com.eldercare.backend.health.internal.threshold.persistence;

import com.eldercare.backend.health.internal.threshold.domain.ResolvedThreshold;
import com.eldercare.backend.health.internal.threshold.domain.ThresholdBand;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/** FR-024: ngưỡng cá nhân ưu tiên cơ sở; khoảng thời gian đóng bên trái, mở bên phải. */
@Repository
public class ApplicableThresholdStore {
    private final JdbcTemplate jdbc;
    private final JsonMapper json;

    public ApplicableThresholdStore(JdbcTemplate jdbc, JsonMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    public ResolvedThreshold resolve(long residentId, long indicatorId, Instant measuredAt) {
        var results = jdbc.query(
                """
                SELECT id,components FROM monitoring_setting
                WHERE kind='THRESHOLD' AND indicator_id=? AND status IN ('EFFECTIVE','EXPIRED')
                    AND ((scope='PERSONAL' AND resident_id=?) OR scope='FACILITY')
                    AND effective_from<=? AND (effective_until IS NULL OR effective_until>?)
                ORDER BY (scope='PERSONAL') DESC,effective_from DESC,id DESC LIMIT 1
                """,
                (rs, row) -> new ResolvedThreshold(
                        rs.getLong("id"),
                        json.readValue(rs.getString("components"), new TypeReference<Map<String, ThresholdBand>>() {})),
                indicatorId,
                residentId,
                Timestamp.from(measuredAt),
                Timestamp.from(measuredAt));
        return results.isEmpty() ? new ResolvedThreshold(null, Map.of()) : results.getFirst();
    }
}
