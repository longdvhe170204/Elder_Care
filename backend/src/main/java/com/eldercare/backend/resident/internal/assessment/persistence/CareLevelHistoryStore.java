package com.eldercare.backend.resident.internal.assessment.persistence;

import com.eldercare.backend.resident.api.dto.CareLevel;
import com.eldercare.backend.resident.api.dto.CareLevelAtDate;
import com.eldercare.backend.resident.api.dto.CareLevelBasisType;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/**
 * Mức chăm sóc hiện hành ({@code resident.current_care_level}) và lịch sử chỉ thêm ({@code care_level_history}; FR-052,
 * FR-053). Hai thay đổi đi cùng nhau trong transaction của lệnh gọi.
 */
@Repository
public class CareLevelHistoryStore {

    private static final RowMapper<CareLevelAtDate> ROW = (rs, i) -> new CareLevelAtDate(
            CareLevel.valueOf(rs.getString("care_level")),
            rs.getObject("effective_from", LocalDateTime.class).toInstant(ZoneOffset.UTC),
            CareLevelBasisType.valueOf(rs.getString("basis_type")),
            rs.getLong("basis_id"));

    private final JdbcTemplate jdbc;

    CareLevelHistoryStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<CareLevel> current(long residentId) {
        return jdbc
                .query("SELECT current_care_level FROM resident WHERE id = ?", (rs, i) -> rs.getString(1), residentId)
                .stream()
                .filter(code -> code != null)
                .map(CareLevel::valueOf)
                .findFirst();
    }

    /** Đặt mức hiện hành và thêm dòng lịch sử. */
    public void apply(
            long residentId,
            CareLevel level,
            Instant effectiveFrom,
            CareLevelBasisType basisType,
            long basisId,
            Instant recordedAt,
            long recordedBy) {
        jdbc.update("UPDATE resident SET current_care_level = ? WHERE id = ?", level.name(), residentId);
        jdbc.update(
                "INSERT INTO care_level_history (resident_id, care_level, effective_from, basis_type, basis_id,"
                        + " recorded_at, recorded_by) VALUES (?, ?, ?, ?, ?, ?, ?)",
                residentId,
                level.name(),
                utc(effectiveFrom),
                basisType.name(),
                basisId,
                utc(recordedAt),
                recordedBy);
    }

    /** Dòng có {@code effective_from} lớn nhất mà không sau {@code at} (FR-053). */
    public Optional<CareLevelAtDate> at(long residentId, Instant at) {
        return jdbc
                .query(
                        "SELECT care_level, effective_from, basis_type, basis_id FROM care_level_history"
                                + " WHERE resident_id = ? AND effective_from <= ?"
                                + " ORDER BY effective_from DESC LIMIT 1",
                        ROW,
                        residentId,
                        utc(at))
                .stream()
                .findFirst();
    }

    public List<CareLevelAtDate> history(long residentId) {
        return jdbc.query(
                "SELECT care_level, effective_from, basis_type, basis_id FROM care_level_history"
                        + " WHERE resident_id = ? ORDER BY effective_from DESC, id DESC",
                ROW,
                residentId);
    }

    private static LocalDateTime utc(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }
}
