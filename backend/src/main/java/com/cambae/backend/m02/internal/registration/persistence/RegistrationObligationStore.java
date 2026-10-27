package com.cambae.backend.m02.internal.registration.persistence;

import com.cambae.backend.m02.internal.registration.domain.ObligationTrigger;
import com.cambae.backend.m02.internal.registration.domain.RegistrationObligation;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Bảng kỹ thuật {@code registration_obligation} (data-model mục 4; research Z-03): mỗi mốc phát sinh một dòng, hạn chốt
 * lúc ghi. Không phải bản ghi nghiệp vụ, không tạo khai báo (Q-306). Thời điểm lưu UTC (DBR-25).
 */
@Repository
public class RegistrationObligationStore {

    private static final String COLUMNS = "id, resident_id, trigger_type, trigger_at, due_at, source_id,"
            + " registration_id, submitted, manager_notified_at";

    private final JdbcTemplate jdbc;

    RegistrationObligationStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Ghi mốc; {@code false} nếu mốc {@code (người, loại mốc, nguồn)} đã có (sự kiện giao lại, NFR-05). Không dùng {@code
     * INSERT IGNORE} để lỗi khác vẫn nổi lên; MySQL chỉ hoàn tác câu lệnh trùng khóa, transaction đi tiếp.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public boolean record(long residentId, ObligationTrigger trigger, Instant triggerAt, Instant dueAt, long sourceId) {
        try {
            jdbc.update(
                    "INSERT INTO registration_obligation (resident_id, trigger_type, trigger_at, due_at, source_id)"
                            + " VALUES (?, ?, ?, ?, ?)",
                    residentId,
                    trigger.name(),
                    ResidenceRegistrationStore.utc(triggerAt),
                    ResidenceRegistrationStore.utc(dueAt),
                    sourceId);
            return true;
        } catch (DuplicateKeyException e) {
            return false;
        }
    }

    /** Nghĩa vụ chưa có khai báo đáp ứng, chưa hủy, hạn sớm nhất trước; đọc khóa dưới khóa của người cao tuổi. */
    @Transactional(propagation = Propagation.MANDATORY)
    public List<RegistrationObligation> openOf(long residentId) {
        return jdbc.query(
                "SELECT " + COLUMNS + " FROM registration_obligation WHERE resident_id = ? AND registration_id IS NULL"
                        + " AND cancelled_at IS NULL ORDER BY due_at, id FOR UPDATE",
                this::row,
                residentId);
    }

    /** Gắn khai báo vừa lập vào nghĩa vụ. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void markMet(long id, long registrationId) {
        jdbc.update(
                "UPDATE registration_obligation SET registration_id = ? WHERE id = ? AND registration_id IS NULL",
                registrationId,
                id);
    }

    /** Nghĩa vụ chưa hủy: chưa lập khai báo, hoặc đã lập mà chưa ghi nộp (danh sách cần làm, FR-001, FR-002). */
    public List<RegistrationObligation> pending() {
        return jdbc.query(
                "SELECT " + COLUMNS + " FROM registration_obligation WHERE cancelled_at IS NULL AND NOT submitted"
                        + " ORDER BY due_at, id",
                this::row);
    }

    /** Nghĩa vụ chưa hủy, chưa ghi nộp, hạn trước {@code before} (quá hạn, FR-002). */
    public List<RegistrationObligation> overdue(Instant before) {
        return jdbc.query(
                "SELECT " + COLUMNS + " FROM registration_obligation WHERE cancelled_at IS NULL AND NOT submitted"
                        + " AND due_at < ? ORDER BY resident_id, due_at, id",
                this::row,
                ResidenceRegistrationStore.utc(before));
    }

    /** Đánh dấu đã báo Quản lý viện; {@code false} nếu đã báo trước đó (một lần, FR-002). */
    @Transactional(propagation = Propagation.MANDATORY)
    public boolean markManagerNotified(long id, Instant at) {
        return jdbc.update(
                        "UPDATE registration_obligation SET manager_notified_at = ? WHERE id = ?"
                                + " AND manager_notified_at IS NULL",
                        ResidenceRegistrationStore.utc(at),
                        id)
                == 1;
    }

    private RegistrationObligation row(ResultSet rs, int i) throws SQLException {
        LocalDateTime notified = rs.getObject("manager_notified_at", LocalDateTime.class);
        return new RegistrationObligation(
                rs.getLong("id"),
                rs.getLong("resident_id"),
                ObligationTrigger.valueOf(rs.getString("trigger_type")),
                rs.getObject("trigger_at", LocalDateTime.class).toInstant(ZoneOffset.UTC),
                rs.getObject("due_at", LocalDateTime.class).toInstant(ZoneOffset.UTC),
                rs.getLong("source_id"),
                rs.getObject("registration_id", Long.class),
                rs.getBoolean("submitted"),
                notified == null ? null : notified.toInstant(ZoneOffset.UTC));
    }
}
