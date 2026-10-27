package com.cambae.backend.m02.internal.registration.persistence;

import com.cambae.backend.m02.internal.registration.domain.ObligationTrigger;
import com.cambae.backend.m02.internal.registration.domain.RegistrationObligation;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
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

    private static final String COLUMNS = "o.id, o.resident_id, o.trigger_type, o.trigger_at, o.due_at, o.source_id,"
            + " o.registration_id, o.submitted, o.manager_notified_at";

    /**
     * Hạn đang có hiệu lực theo trạng thái (quyết định Phase 4, chủ dự án chốt 2026-10-27): khai báo của nghĩa vụ đã bị từ
     * chối và đang chờ nộp lại (Cần khai báo, lần nộp hiện hành {@code REJECTED}) thì hạn mới của khai báo (lúc ghi từ chối
     * + CFG-M02-12, FR-005); chưa lập khai báo, hoặc khai báo chưa từng bị từ chối, thì hạn của nghĩa vụ (FR-002). Không
     * lấy hạn khai báo khi chỉ vì có khai báo: hạn của khai báo là hạn nghĩa vụ sớm nhất lúc lập, khác hạn từng nghĩa vụ.
     */
    private static final String FROM_WITH_EFFECTIVE_DUE = ", CASE WHEN r.status = 'TO_SUBMIT' AND s.result = 'REJECTED'"
            + " THEN r.due_at ELSE o.due_at END AS effective_due_at FROM registration_obligation o"
            + " LEFT JOIN residence_registration r ON r.id = o.registration_id"
            + " LEFT JOIN registration_submission s ON s.id = r.current_submission_id";

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
                "SELECT " + COLUMNS + ", o.due_at AS effective_due_at FROM registration_obligation o"
                        + " WHERE o.resident_id = ? AND o.registration_id IS NULL AND o.cancelled_at IS NULL"
                        + " ORDER BY o.due_at, o.id FOR UPDATE",
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

    /** Nghĩa vụ {@code id}, kèm hạn đang có hiệu lực. */
    public Optional<RegistrationObligation> find(long id) {
        return jdbc.query("SELECT " + COLUMNS + FROM_WITH_EFFECTIVE_DUE + " WHERE o.id = ?", this::row, id).stream()
                .findFirst();
    }

    /**
     * Nghĩa vụ chưa hủy: chưa lập khai báo, hoặc đã lập mà chưa ghi nộp, hoặc khai báo bị từ chối chờ nộp lại (danh sách
     * cần làm, FR-001, FR-002).
     */
    public List<RegistrationObligation> pending() {
        return jdbc.query(
                "SELECT " + COLUMNS + FROM_WITH_EFFECTIVE_DUE + " WHERE o.cancelled_at IS NULL AND NOT o.submitted"
                        + " ORDER BY effective_due_at, o.id",
                this::row);
    }

    /** Nghĩa vụ chưa hủy, chưa ghi nộp, hạn đang có hiệu lực trước {@code before} (quá hạn, FR-002). */
    public List<RegistrationObligation> overdue(Instant before) {
        return jdbc.query(
                "SELECT " + COLUMNS + FROM_WITH_EFFECTIVE_DUE + " WHERE o.cancelled_at IS NULL AND NOT o.submitted"
                        + " AND (CASE WHEN r.status = 'TO_SUBMIT' AND s.result = 'REJECTED' THEN r.due_at"
                        + " ELSE o.due_at END) < ? ORDER BY o.resident_id, effective_due_at, o.id",
                this::row,
                ResidenceRegistrationStore.utc(before));
    }

    /**
     * Đặt cờ đã ghi nộp của mọi nghĩa vụ mà khai báo {@code registrationId} đáp ứng: {@code true} khi ghi đã nộp (dừng
     * nhắc quá hạn), {@code false} khi lần nộp bị từ chối (không còn lần nộp đang chờ). Hạn gốc, {@code
     * manager_notified_at} không đổi.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void markSubmitted(long registrationId, boolean submitted) {
        jdbc.update(
                "UPDATE registration_obligation SET submitted = ? WHERE registration_id = ? AND cancelled_at IS NULL",
                submitted,
                registrationId);
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
                notified == null ? null : notified.toInstant(ZoneOffset.UTC),
                rs.getObject("effective_due_at", LocalDateTime.class).toInstant(ZoneOffset.UTC));
    }
}
