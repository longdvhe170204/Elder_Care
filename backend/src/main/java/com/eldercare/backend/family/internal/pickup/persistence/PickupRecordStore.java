package com.eldercare.backend.family.internal.pickup.persistence;

import com.eldercare.backend.family.internal.pickup.domain.PickupOutcome;
import com.eldercare.backend.family.internal.pickup.domain.PickupRecord;
import com.eldercare.backend.family.internal.pickup.domain.PickupType;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

/**
 * Đọc, ghi {@code pickup_record} (V012_04, nhóm 3: chỉ INSERT). Kết quả Đã dùng, Không dùng là {@code record_event} theo
 * khóa {@link PickupOutcome#key}; truy vấn "chưa có kết quả" dựa vào khóa đó.
 */
@Repository
public class PickupRecordStore {

    private static final String SELECT = "SELECT id, resident_id, type, family_member_id, basis, exception_id,"
            + " picker_name, id_document_type, id_document_number, at, handover_staff_id, recorded_by, expires_at"
            + " FROM pickup_record";
    private static final String NO_OUTCOME = " NOT EXISTS (SELECT 1 FROM record_event e WHERE e.unique_key ="
            + " CONCAT('M10.PICKUP_DONE:', pickup_record.id))";

    private final JdbcTemplate jdbc;

    PickupRecordStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public long insert(PickupRecord r, Instant createdAt) {
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(
                c -> {
                    PreparedStatement s = c.prepareStatement(
                            "INSERT INTO pickup_record (resident_id, type, family_member_id, basis, exception_id,"
                                    + " picker_name, id_document_type, id_document_number, at, handover_staff_id,"
                                    + " recorded_by, expires_at, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,"
                                    + " ?)",
                            Statement.RETURN_GENERATED_KEYS);
                    s.setLong(1, r.residentId());
                    s.setString(2, r.type().name());
                    s.setObject(3, r.familyMemberId(), Types.BIGINT);
                    s.setString(4, r.basis().name());
                    s.setObject(5, r.exceptionId(), Types.BIGINT);
                    s.setString(6, r.pickerName());
                    s.setString(7, r.idDocumentType());
                    s.setString(8, r.idDocumentNumber());
                    s.setObject(9, utc(r.at()));
                    s.setLong(10, r.handoverStaffId());
                    s.setLong(11, r.recordedBy());
                    s.setObject(12, r.expiresAt() == null ? null : utc(r.expiresAt()));
                    s.setObject(13, utc(createdAt));
                    return s;
                },
                keys);
        return keys.getKey().longValue();
    }

    public Optional<PickupRecord> find(long id) {
        return jdbc.query(SELECT + " WHERE id = ?", PickupRecordStore::row, id).stream()
                .findFirst();
    }

    /** Mới nhất trước. */
    public List<PickupRecord> ofResident(long residentId) {
        return jdbc.query(
                SELECT + " WHERE resident_id = ? ORDER BY at DESC, id DESC", PickupRecordStore::row, residentId);
    }

    /** Bản ghi chưa có kết quả của người đón có hồ sơ (FR-017: bỏ người đón thì chuyển Không dùng). */
    public List<PickupRecord> withoutOutcome(long residentId, long familyMemberId) {
        return jdbc.query(
                SELECT + " WHERE resident_id = ? AND family_member_id = ? AND" + NO_OUTCOME,
                PickupRecordStore::row,
                residentId,
                familyMemberId);
    }

    /** Bản ghi Tự về chưa có kết quả (tắt dấu "được tự về"). */
    public List<PickupRecord> selfReturnsWithoutOutcome(long residentId) {
        return jdbc.query(
                SELECT + " WHERE resident_id = ? AND type = ? AND" + NO_OUTCOME,
                PickupRecordStore::row,
                residentId,
                PickupType.SELF_RETURN.name());
    }

    /** Bản ghi chưa có kết quả đã tới hạn CFG-M10-09 (FR-027). */
    public List<PickupRecord> expiredWithoutOutcome(Instant at) {
        return jdbc.query(
                SELECT + " WHERE expires_at <= ? AND" + NO_OUTCOME + " ORDER BY expires_at, id",
                PickupRecordStore::row,
                utc(at));
    }

    private static PickupRecord row(ResultSet rs, int n) throws SQLException {
        return new PickupRecord(
                rs.getLong("id"),
                rs.getLong("resident_id"),
                PickupType.valueOf(rs.getString("type")),
                rs.getObject("family_member_id", Long.class),
                PickupRecord.Basis.valueOf(rs.getString("basis")),
                rs.getObject("exception_id", Long.class),
                rs.getString("picker_name"),
                rs.getString("id_document_type"),
                rs.getString("id_document_number"),
                instant(rs, "at"),
                rs.getLong("handover_staff_id"),
                rs.getLong("recorded_by"),
                instant(rs, "expires_at"));
    }

    private static LocalDateTime utc(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    private static Instant instant(ResultSet rs, String column) throws SQLException {
        LocalDateTime value = rs.getObject(column, LocalDateTime.class);
        return value == null ? null : value.toInstant(ZoneOffset.UTC);
    }
}
