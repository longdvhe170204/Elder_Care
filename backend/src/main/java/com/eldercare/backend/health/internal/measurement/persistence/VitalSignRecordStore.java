package com.eldercare.backend.health.internal.measurement.persistence;

import com.eldercare.backend.health.api.dto.VitalSignConfirmationStatus;
import com.eldercare.backend.health.api.dto.VitalSignLabel;
import com.eldercare.backend.health.api.dto.VitalSignLevel;
import com.eldercare.backend.health.internal.measurement.domain.VitalSignComponent;
import com.eldercare.backend.health.internal.measurement.domain.VitalSignRecord;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/** Chỉ đọc bảng do M06 sở hữu; đính chính và danh mục đi qua API. */
@Repository
public class VitalSignRecordStore {
    private final JdbcTemplate jdbc;
    private final JsonMapper json;

    public VitalSignRecordStore(JdbcTemplate jdbc, JsonMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    public List<VitalSignRecord> confirmedForResident(long residentId) {
        // Giữ cả bản cũ để có thể quay lại bản hợp lệ sau khi bản mới nhất bị hủy.
        return jdbc.query(
                "SELECT * FROM vital_sign_record WHERE resident_id=? AND confirmation_status='CONFIRMED' ORDER BY measured_at DESC,id DESC",
                this::row,
                residentId);
    }

    public List<VitalSignRecord> withinRange(long residentId, long indicatorId, Instant from, Instant to) {
        return jdbc.query(
                "SELECT * FROM vital_sign_record WHERE resident_id=? AND indicator_id=? AND confirmed_at IS NOT NULL AND measured_at>=? AND measured_at<? ORDER BY measured_at,id",
                this::row,
                residentId,
                indicatorId,
                Timestamp.from(from),
                Timestamp.from(to));
    }

    public List<VitalSignRecord> aboveThreshold(Instant from, Instant to) {
        return jdbc.query(
                "SELECT * FROM vital_sign_record WHERE confirmed_at IS NOT NULL AND level IN ('WARNING','DANGEROUS') AND measured_at>=? AND measured_at<? ORDER BY measured_at,id",
                this::row,
                Timestamp.from(from),
                Timestamp.from(to));
    }

    private VitalSignRecord row(ResultSet rs, int rowNum) throws SQLException {
        String labels = rs.getString("labels");
        Set<VitalSignLabel> parsedLabels = labels.isEmpty()
                ? Set.of()
                : Arrays.stream(labels.split(",")).map(VitalSignLabel::valueOf).collect(Collectors.toUnmodifiableSet());
        Map<String, VitalSignComponent> components =
                json.readValue(rs.getString("components"), new TypeReference<>() {});
        return new VitalSignRecord(
                rs.getLong("id"),
                rs.getLong("resident_id"),
                rs.getLong("indicator_id"),
                rs.getTimestamp("measured_at").toInstant(),
                VitalSignLevel.valueOf(rs.getString("level")),
                VitalSignConfirmationStatus.valueOf(rs.getString("confirmation_status")),
                components,
                parsedLabels,
                rs.getTimestamp("locked_at").toInstant());
    }
}
