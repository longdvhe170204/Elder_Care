package com.cambae.backend.m02.internal.registration.persistence;

import com.cambae.backend.m02.internal.registration.domain.RegistrationSubmission;
import com.cambae.backend.m02.internal.registration.domain.SubmissionChannel;
import com.cambae.backend.m02.internal.registration.domain.SubmissionResult;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Bảng {@code registration_submission} (nhóm 3, FR-003): mỗi lần ghi đã nộp một dòng, không sửa, không xóa (trigger của
 * V018_01); chỉ kết quả đặt một lần từ {@code PENDING}. Thời điểm lưu UTC (DBR-25).
 */
@Repository
public class RegistrationSubmissionStore {

    private final JdbcTemplate jdbc;

    RegistrationSubmissionStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Thêm lần nộp ở {@code PENDING}; trả mã lần nộp. */
    @Transactional(propagation = Propagation.MANDATORY)
    public long add(
            long registrationId,
            LocalDate submittedDate,
            SubmissionChannel channel,
            String receivingAuthority,
            String dossierCode,
            long recordedBy,
            Instant recordedAt) {
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(
                connection -> {
                    var statement = connection.prepareStatement(
                            "INSERT INTO registration_submission (registration_id, submitted_date, channel,"
                                    + " receiving_authority, dossier_code, recorded_by, recorded_at)"
                                    + " VALUES (?, ?, ?, ?, ?, ?, ?)",
                            Statement.RETURN_GENERATED_KEYS);
                    statement.setLong(1, registrationId);
                    statement.setObject(2, submittedDate);
                    statement.setString(3, channel.name());
                    statement.setString(4, receivingAuthority);
                    statement.setString(5, dossierCode);
                    statement.setLong(6, recordedBy);
                    statement.setObject(7, ResidenceRegistrationStore.utc(recordedAt));
                    return statement;
                },
                keys);
        return keys.getKey().longValue();
    }

    public Optional<RegistrationSubmission> find(long id) {
        return jdbc.query("SELECT * FROM registration_submission WHERE id = ?", this::row, id).stream()
                .findFirst();
    }

    /** Đặt kết quả; {@code false} nếu lần nộp đã có kết quả. */
    @Transactional(propagation = Propagation.MANDATORY)
    public boolean setResult(long id, SubmissionResult result, String rejectionReason) {
        return jdbc.update(
                        "UPDATE registration_submission SET result = ?, rejection_reason = ? WHERE id = ?"
                                + " AND result = 'PENDING'",
                        result.name(),
                        rejectionReason,
                        id)
                == 1;
    }

    private RegistrationSubmission row(ResultSet rs, int i) throws SQLException {
        return new RegistrationSubmission(
                rs.getLong("id"),
                rs.getLong("registration_id"),
                rs.getObject("submitted_date", LocalDate.class),
                SubmissionChannel.valueOf(rs.getString("channel")),
                rs.getString("receiving_authority"),
                rs.getString("dossier_code"),
                SubmissionResult.valueOf(rs.getString("result")),
                rs.getString("rejection_reason"),
                rs.getLong("recorded_by"),
                rs.getObject("recorded_at", LocalDateTime.class).toInstant(ZoneOffset.UTC));
    }
}
