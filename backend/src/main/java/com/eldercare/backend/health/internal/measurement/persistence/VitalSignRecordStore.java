package com.eldercare.backend.health.internal.measurement.persistence;

import com.eldercare.backend.common.error.ConditionNotMetException;
import com.eldercare.backend.common.error.UnmetCondition;
import com.eldercare.backend.health.api.dto.VitalSignConfirmationStatus;
import com.eldercare.backend.health.api.dto.VitalSignLabel;
import com.eldercare.backend.health.api.dto.VitalSignLevel;
import com.eldercare.backend.health.internal.measurement.domain.VitalSignComponent;
import com.eldercare.backend.health.internal.measurement.domain.VitalSignRecord;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/** Chỉ đọc và ghi bảng do M06 sở hữu; đính chính và danh mục đi qua API. */
@Repository
public class VitalSignRecordStore {
    private final JdbcTemplate jdbc;
    private final JsonMapper json;

    public VitalSignRecordStore(JdbcTemplate jdbc, JsonMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    public Optional<VitalSignRecord> findById(long id) {
        List<VitalSignRecord> list = jdbc.query("SELECT * FROM vital_sign_record WHERE id = ?", this::row, id);
        return list.stream().findFirst();
    }

    /** Các bản nháp đã hết hạn, theo thứ tự hạn để job xử lý từng dòng. */
    public List<Long> overdueRemeasurementIds(Instant now) {
        return jdbc.queryForList(
                "SELECT id FROM vital_sign_record WHERE confirmation_status='AWAITING_REMEASUREMENT'"
                        + " AND remeasurement_deadline<=? ORDER BY remeasurement_deadline,id",
                Long.class,
                Timestamp.from(now));
    }

    public List<VitalSignRecord> confirmedForResident(long residentId) {
        // Giữ cả bản cũ để có thể quay lại bản hợp lệ sau khi bản mới nhất bị hủy.
        return jdbc.query(
                "SELECT * FROM vital_sign_record WHERE resident_id=? AND confirmation_status='CONFIRMED' ORDER BY measured_at DESC,id DESC",
                this::row,
                residentId);
    }

    public List<VitalSignRecord> confirmedInPeriod(Instant from, Instant to) {
        return jdbc.query(
                "SELECT * FROM vital_sign_record WHERE confirmation_status='CONFIRMED' AND measured_at>=? AND measured_at<? ORDER BY measured_at,id",
                this::row,
                Timestamp.from(from),
                Timestamp.from(to));
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

    public VitalSignRecord insert(
            long residentId,
            long indicatorId,
            Instant measuredAt,
            long measuredBy,
            Long taskId,
            Instant deviceMeasuredAt,
            boolean offline,
            Long thresholdVersionId,
            VitalSignLevel level,
            VitalSignConfirmationStatus confirmationStatus,
            Instant remeasurementDeadline,
            Long remeasurementFor,
            Map<String, VitalSignComponent> components,
            Set<VitalSignLabel> labels,
            String note,
            Instant confirmedAt,
            Instant lockedAt,
            Instant createdAt,
            long createdBy,
            boolean syncedAfterPermissionLoss) {
        String componentsJson = json.writeValueAsString(components);
        String labelsStr = labels.stream().map(Enum::name).collect(Collectors.joining(","));
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(
                connection -> {
                    PreparedStatement ps = connection.prepareStatement(
                            "INSERT INTO vital_sign_record ("
                                    + "resident_id, indicator_id, measured_at, measured_by, task_id, "
                                    + "device_measured_at, offline, threshold_version_id, level, "
                                    + "confirmation_status, remeasurement_deadline, remeasurement_for, "
                                    + "components, labels, note, confirmed_at, locked_at, created_at, created_by, "
                                    + "synced_after_permission_loss) "
                                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                            Statement.RETURN_GENERATED_KEYS);
                    ps.setLong(1, residentId);
                    ps.setLong(2, indicatorId);
                    ps.setTimestamp(3, Timestamp.from(measuredAt));
                    ps.setLong(4, measuredBy);
                    if (taskId == null) {
                        ps.setNull(5, Types.BIGINT);
                    } else {
                        ps.setLong(5, taskId);
                    }
                    if (deviceMeasuredAt == null) {
                        ps.setNull(6, Types.TIMESTAMP);
                    } else {
                        ps.setTimestamp(6, Timestamp.from(deviceMeasuredAt));
                    }
                    ps.setBoolean(7, offline);
                    if (thresholdVersionId == null) {
                        ps.setNull(8, Types.BIGINT);
                    } else {
                        ps.setLong(8, thresholdVersionId);
                    }
                    ps.setString(9, level.name());
                    ps.setString(10, confirmationStatus.name());
                    if (remeasurementDeadline == null) {
                        ps.setNull(11, Types.TIMESTAMP);
                    } else {
                        ps.setTimestamp(11, Timestamp.from(remeasurementDeadline));
                    }
                    if (remeasurementFor == null) {
                        ps.setNull(12, Types.BIGINT);
                    } else {
                        ps.setLong(12, remeasurementFor);
                    }
                    ps.setString(13, componentsJson);
                    ps.setString(14, labelsStr);
                    ps.setString(15, note);
                    if (confirmedAt == null) {
                        ps.setNull(16, Types.TIMESTAMP);
                    } else {
                        ps.setTimestamp(16, Timestamp.from(confirmedAt));
                    }
                    if (lockedAt == null) {
                        ps.setNull(17, Types.TIMESTAMP);
                    } else {
                        ps.setTimestamp(17, Timestamp.from(lockedAt));
                    }
                    ps.setTimestamp(18, Timestamp.from(createdAt));
                    ps.setLong(19, createdBy);
                    ps.setBoolean(20, syncedAfterPermissionLoss);
                    return ps;
                },
                keyHolder);
        long id = keyHolder.getKey().longValue();
        return new VitalSignRecord(
                id,
                residentId,
                indicatorId,
                measuredAt,
                measuredBy,
                taskId,
                deviceMeasuredAt,
                offline,
                thresholdVersionId,
                level,
                confirmationStatus,
                remeasurementDeadline,
                remeasurementFor,
                components,
                labels,
                note,
                confirmedAt,
                lockedAt,
                createdAt,
                createdBy);
    }

    public void confirmRemeasurement(long originalId, Instant confirmedAt, Set<VitalSignLabel> labels) {
        String labelsStr = labels.stream().map(Enum::name).collect(Collectors.joining(","));
        int updated = jdbc.update(
                "UPDATE vital_sign_record SET confirmation_status = 'REMEASURED', confirmed_at = ?, locked_at = ?, labels = ? WHERE id = ? AND confirmation_status='AWAITING_REMEASUREMENT'",
                Timestamp.from(confirmedAt),
                Timestamp.from(confirmedAt),
                labelsStr,
                originalId);
        if (updated == 0) {
            throw new ConditionNotMetException(List.of(
                    new UnmetCondition("RECORD_NOT_AWAITING_REMEASUREMENT", "Bản ghi không ở trạng thái chờ đo lại")));
        }
    }

    /** Chuyển bản nháp quá hạn thành bản đã xác nhận đúng một lần, tránh đua với lệnh đo lại. */
    public boolean confirmOverdue(long id, Instant deadline, Instant confirmedAt, Set<VitalSignLabel> labels) {
        String labelsStr = labels.stream().map(Enum::name).collect(Collectors.joining(","));
        return jdbc.update(
                        "UPDATE vital_sign_record SET confirmation_status='CONFIRMED', confirmed_at=?, locked_at=?,"
                                + " labels=? WHERE id=? AND confirmation_status='AWAITING_REMEASUREMENT'"
                                + " AND remeasurement_deadline<=?",
                        Timestamp.from(confirmedAt),
                        Timestamp.from(confirmedAt),
                        labelsStr,
                        id,
                        Timestamp.from(deadline))
                == 1;
    }

    private VitalSignRecord row(ResultSet rs, int rowNum) throws SQLException {
        String labels = rs.getString("labels");
        Set<VitalSignLabel> parsedLabels = labels == null || labels.isEmpty()
                ? Set.of()
                : Arrays.stream(labels.split(",")).map(VitalSignLabel::valueOf).collect(Collectors.toUnmodifiableSet());
        Map<String, VitalSignComponent> components =
                json.readValue(rs.getString("components"), new TypeReference<>() {});
        Timestamp devMeasuredAt = rs.getTimestamp("device_measured_at");
        Timestamp remeasureDeadline = rs.getTimestamp("remeasurement_deadline");
        Timestamp confirmedAt = rs.getTimestamp("confirmed_at");
        Timestamp lockedAt = rs.getTimestamp("locked_at");
        Timestamp createdAt = rs.getTimestamp("created_at");
        long taskIdVal = rs.getLong("task_id");
        Long taskId = rs.wasNull() ? null : taskIdVal;
        long remeasureForVal = rs.getLong("remeasurement_for");
        Long remeasureFor = rs.wasNull() ? null : remeasureForVal;
        long threshVerVal = rs.getLong("threshold_version_id");
        Long threshVer = rs.wasNull() ? null : threshVerVal;

        return new VitalSignRecord(
                rs.getLong("id"),
                rs.getLong("resident_id"),
                rs.getLong("indicator_id"),
                rs.getTimestamp("measured_at").toInstant(),
                rs.getLong("measured_by"),
                taskId,
                devMeasuredAt == null ? null : devMeasuredAt.toInstant(),
                rs.getBoolean("offline"),
                threshVer,
                VitalSignLevel.valueOf(rs.getString("level")),
                VitalSignConfirmationStatus.valueOf(rs.getString("confirmation_status")),
                remeasureDeadline == null ? null : remeasureDeadline.toInstant(),
                remeasureFor,
                components,
                parsedLabels,
                rs.getString("note"),
                confirmedAt == null ? null : confirmedAt.toInstant(),
                lockedAt == null ? null : lockedAt.toInstant(),
                createdAt == null ? null : createdAt.toInstant(),
                rs.getLong("created_by"));
    }
}
