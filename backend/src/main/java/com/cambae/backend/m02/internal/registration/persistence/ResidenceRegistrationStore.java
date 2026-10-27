package com.cambae.backend.m02.internal.registration.persistence;

import com.cambae.backend.m02.internal.registration.domain.RegistrationStatus;
import com.cambae.backend.m02.internal.registration.domain.RegistrationType;
import com.cambae.backend.m02.internal.registration.domain.ResidenceRegistration;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Đọc, ghi bảng {@code residence_registration}. Quy ước tên (plan 018 "Phase 1 hoàn thành", glossary 24.5): mọi lệnh ghi
 * dòng khai báo mới đặt tên {@code insert…} để {@code ResidenceRegistrationArchTest} bắt được đường đi từ Bộ lập lịch.
 * Thời điểm lưu UTC (DBR-25).
 */
@Repository
public class ResidenceRegistrationStore {

    private static final String COLUMNS = "id, resident_id, type, renewal, obligation_id, due_at,"
            + " local_permanent_resident, basis_note, status, expiry_date, current_submission_id, evidence_file_id";

    private final JdbcTemplate jdbc;

    ResidenceRegistrationStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Người cao tuổi của khai báo, rỗng nếu không có khai báo {@code id}. */
    public Optional<Long> residentOf(long id) {
        return jdbc
                .query("SELECT resident_id FROM residence_registration WHERE id = ?", (rs, i) -> rs.getLong(1), id)
                .stream()
                .findFirst();
    }

    /**
     * Ghi khai báo mới ở Cần khai báo (lệnh lập khai báo của Hành chính, Q-306). Khóa sinh {@code in_progress_key} chặn khai
     * báo đang xử lý thứ hai (DBR-33) bằng {@code DuplicateKeyException}.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public long insertToSubmit(
            long residentId,
            RegistrationType type,
            boolean renewal,
            Long obligationId,
            Instant dueAt,
            boolean localPermanentResident,
            String basisNote,
            Long replacedRegistrationId) {
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(
                connection -> {
                    var statement = connection.prepareStatement(
                            "INSERT INTO residence_registration (resident_id, type, renewal, obligation_id, due_at,"
                                    + " local_permanent_resident, basis_note, replaced_registration_id)"
                                    + " VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                            Statement.RETURN_GENERATED_KEYS);
                    statement.setLong(1, residentId);
                    statement.setString(2, type.name());
                    statement.setBoolean(3, renewal);
                    statement.setObject(4, obligationId);
                    statement.setObject(5, utc(dueAt));
                    statement.setBoolean(6, localPermanentResident);
                    statement.setString(7, basisNote);
                    statement.setObject(8, replacedRegistrationId);
                    return statement;
                },
                keys);
        return keys.getKey().longValue();
    }

    public Optional<ResidenceRegistration> find(long id) {
        return jdbc.query("SELECT " + COLUMNS + " FROM residence_registration WHERE id = ?", this::row, id).stream()
                .findFirst();
    }

    /** Đọc khóa khai báo (bản mới nhất đã commit), dùng sau khi đã khóa {@code registration_resident}. */
    @Transactional(propagation = Propagation.MANDATORY)
    public Optional<ResidenceRegistration> findForUpdate(long id) {
        return jdbc
                .query("SELECT " + COLUMNS + " FROM residence_registration WHERE id = ? FOR UPDATE", this::row, id)
                .stream()
                .findFirst();
    }

    /** Khai báo Đã xác nhận của người cao tuổi (tối đa một, DBR-33), đọc khóa. */
    @Transactional(propagation = Propagation.MANDATORY)
    public Optional<Long> confirmedOf(long residentId) {
        return jdbc
                .queryForList(
                        "SELECT id FROM residence_registration WHERE confirmed_key = ? FOR UPDATE",
                        Long.class,
                        residentId)
                .stream()
                .findFirst();
    }

    /** Đăng ký tạm trú Đã xác nhận (nhắc gia hạn, chuyển Hết hạn; FR-006). */
    public List<ResidenceRegistration> confirmedTemporaryResidence() {
        return jdbc.query(
                "SELECT " + COLUMNS + " FROM residence_registration WHERE status = 'CONFIRMED'"
                        + " AND type = 'TEMPORARY_RESIDENCE_REGISTRATION' ORDER BY id",
                this::row);
    }

    /** Người cao tuổi đang có khai báo Cần khai báo hoặc Đã nộp (FR-004). */
    public java.util.Set<Long> residentsWithInProgress() {
        return new java.util.HashSet<>(jdbc.queryForList(
                "SELECT resident_id FROM residence_registration WHERE in_progress_key IS NOT NULL", Long.class));
    }

    /** Khai báo của người cao tuổi, mới nhất trước. */
    public List<ResidenceRegistration> ofResident(long residentId) {
        return jdbc.query(
                "SELECT " + COLUMNS + " FROM residence_registration WHERE resident_id = ? ORDER BY id DESC",
                this::row,
                residentId);
    }

    /**
     * Có khai báo Cần khai báo hoặc Đã nộp (FR-004). Đọc khóa (bản mới nhất đã commit, không theo ảnh chụp của transaction)
     * vì bên gọi vừa chờ khóa {@code registration_resident} của lệnh khác.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public boolean hasInProgress(long residentId) {
        return !jdbc.queryForList(
                        "SELECT id FROM residence_registration WHERE in_progress_key = ? FOR UPDATE",
                        Long.class,
                        residentId)
                .isEmpty();
    }

    /** Xác nhận thường trú cùng xã/phường (FR-001): loại thành Thông báo lưu trú, hạn giữ nguyên; chỉ khi Cần khai báo. */
    @Transactional(propagation = Propagation.MANDATORY)
    public boolean confirmLocalPermanentResident(long id, String basisNote) {
        return jdbc.update(
                        "UPDATE residence_registration SET type = 'STAY_NOTIFICATION', local_permanent_resident = TRUE,"
                                + " basis_note = ? WHERE id = ? AND status = 'TO_SUBMIT'",
                        basisNote,
                        id)
                == 1;
    }

    /**
     * Gỡ xác nhận thường trú cùng xã/phường bằng đính chính (FR-001): loại tính lại, ghi chú căn cứ bỏ; chỉ khi Cần khai
     * báo.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public boolean removeLocalPermanentResident(long id, RegistrationType type) {
        return jdbc.update(
                        "UPDATE residence_registration SET type = ?, local_permanent_resident = FALSE, basis_note = NULL"
                                + " WHERE id = ? AND status = 'TO_SUBMIT'",
                        type.name(),
                        id)
                == 1;
    }

    /** Ghi đã nộp (FR-005): Cần khai báo → Đã nộp, lần nộp hiện hành. */
    @Transactional(propagation = Propagation.MANDATORY)
    public boolean markSubmitted(long id, long submissionId) {
        return jdbc.update(
                        "UPDATE residence_registration SET status = 'SUBMITTED', current_submission_id = ?"
                                + " WHERE id = ? AND status = 'TO_SUBMIT'",
                        submissionId,
                        id)
                == 1;
    }

    /** Ghi được chấp nhận (FR-005): Đã nộp → Đã xác nhận, ngày hết hạn, bằng chứng. */
    @Transactional(propagation = Propagation.MANDATORY)
    public boolean markConfirmed(long id, LocalDate expiryDate, long evidenceFileId) {
        return jdbc.update(
                        "UPDATE residence_registration SET status = 'CONFIRMED', expiry_date = ?, evidence_file_id = ?"
                                + " WHERE id = ? AND status = 'SUBMITTED'",
                        expiryDate,
                        evidenceFileId,
                        id)
                == 1;
    }

    /** Ghi bị từ chối (FR-005): Đã nộp → Cần khai báo, hạn mới của khai báo; lần nộp hiện hành giữ (đã từ chối). */
    @Transactional(propagation = Propagation.MANDATORY)
    public boolean markRejected(long id, Instant newDueAt) {
        return jdbc.update(
                        "UPDATE residence_registration SET status = 'TO_SUBMIT', due_at = ?"
                                + " WHERE id = ? AND status = 'SUBMITTED'",
                        utc(newDueAt),
                        id)
                == 1;
    }

    /** Qua ngày hết hạn (FR-006): Đã xác nhận → Hết hạn; {@code false} nếu đã chuyển. */
    @Transactional(propagation = Propagation.MANDATORY)
    public boolean markExpired(long id) {
        return jdbc.update(
                        "UPDATE residence_registration SET status = 'EXPIRED' WHERE id = ? AND status = 'CONFIRMED'",
                        id)
                == 1;
    }

    /** Khai báo Đã xác nhận cũ → Được thay thế (FR-005), trước khi khai báo mới Đã xác nhận, cùng transaction. */
    @Transactional(propagation = Propagation.MANDATORY)
    public boolean markSuperseded(long id) {
        return jdbc.update(
                        "UPDATE residence_registration SET status = 'SUPERSEDED' WHERE id = ? AND status = 'CONFIRMED'",
                        id)
                == 1;
    }

    private ResidenceRegistration row(ResultSet rs, int i) throws SQLException {
        Date expiry = rs.getDate("expiry_date");
        return new ResidenceRegistration(
                rs.getLong("id"),
                rs.getLong("resident_id"),
                RegistrationType.valueOf(rs.getString("type")),
                rs.getBoolean("renewal"),
                rs.getObject("obligation_id", Long.class),
                rs.getObject("due_at", LocalDateTime.class).toInstant(ZoneOffset.UTC),
                rs.getBoolean("local_permanent_resident"),
                rs.getString("basis_note"),
                RegistrationStatus.valueOf(rs.getString("status")),
                expiry == null ? null : expiry.toLocalDate(),
                rs.getObject("current_submission_id", Long.class),
                rs.getObject("evidence_file_id", Long.class));
    }

    static LocalDateTime utc(Instant instant) {
        return instant == null ? null : LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }
}
