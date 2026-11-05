package com.eldercare.backend.family.internal.feedback.persistence;

import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.family.internal.feedback.domain.Feedback;
import com.eldercare.backend.family.internal.feedback.domain.Feedback.AssigneeRole;
import com.eldercare.backend.family.internal.feedback.domain.Feedback.CloseMethod;
import com.eldercare.backend.family.internal.feedback.domain.Feedback.ContentGroup;
import com.eldercare.backend.family.internal.feedback.domain.Feedback.Priority;
import com.eldercare.backend.family.internal.feedback.domain.Feedback.Source;
import com.eldercare.backend.family.internal.feedback.domain.Feedback.Status;
import com.eldercare.backend.family.internal.feedback.domain.Feedback.Type;
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
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

/** Đọc, ghi {@code feedback} (V012_07, nhóm 2); đổi trạng thái kiểm {@code version}. */
@Repository
public class FeedbackStore {

    private static final String SELECT = "SELECT * FROM feedback";
    private static final String OPEN = "('NEW', 'IN_PROGRESS', 'REOPENED')";

    private final JdbcTemplate jdbc;

    FeedbackStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public long insert(Feedback f, Instant createdAt) {
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(
                c -> {
                    PreparedStatement s = c.prepareStatement(
                            "INSERT INTO feedback (resident_id, family_member_id, type, content_group, source,"
                                    + " submitted_at, priority, due_basis_at, due_at, assignee_role,"
                                    + " assignee_account_id, status, recorded_by, created_at) VALUES (?, ?, ?, ?, ?, ?,"
                                    + " ?, ?, ?, ?, ?, ?, ?, ?)",
                            Statement.RETURN_GENERATED_KEYS);
                    s.setLong(1, f.residentId());
                    s.setLong(2, f.familyMemberId());
                    s.setString(3, f.type().name());
                    s.setString(4, f.group().name());
                    s.setString(5, f.source().name());
                    s.setObject(6, utc(f.submittedAt()));
                    s.setString(7, f.priority().name());
                    s.setObject(8, utc(f.dueBasisAt()));
                    s.setObject(9, utc(f.dueAt()));
                    s.setString(10, f.assigneeRole().name());
                    s.setObject(11, f.assigneeAccountId(), Types.BIGINT);
                    s.setString(12, f.status().name());
                    s.setLong(13, f.recordedBy());
                    s.setObject(14, utc(createdAt));
                    return s;
                },
                keys);
        return keys.getKey().longValue();
    }

    public Optional<Feedback> find(long id) {
        return jdbc.query(SELECT + " WHERE id = ?", FeedbackStore::row, id).stream()
                .findFirst();
    }

    public Feedback require(long id) {
        return find(id).orElseThrow(NotFoundException::new);
    }

    public Feedback lockForUpdate(long id) {
        return jdbc.query(SELECT + " WHERE id = ? FOR UPDATE", FeedbackStore::row, id).stream()
                .findFirst()
                .orElseThrow(NotFoundException::new);
    }

    /** Ghi trạng thái, mức, hạn, người phụ trách; 409 {@code STATE_CHANGED} khi phiên bản đã đổi. */
    public Feedback update(Feedback f) {
        int rows = jdbc.update(
                "UPDATE feedback SET priority = ?, due_basis_at = ?, due_at = ?, assignee_role = ?,"
                        + " assignee_account_id = ?, status = ?, overdue = ?, escalated_due_at = ?, replied_at = ?,"
                        + " closed_at = ?, close_method = ?, version = version + 1 WHERE id = ? AND version = ?",
                f.priority().name(),
                utc(f.dueBasisAt()),
                utc(f.dueAt()),
                f.assigneeRole().name(),
                f.assigneeAccountId(),
                f.status().name(),
                f.overdue(),
                utcOrNull(f.escalatedDueAt()),
                utcOrNull(f.repliedAt()),
                utcOrNull(f.closedAt()),
                f.closeMethod() == null ? null : f.closeMethod().name(),
                f.id(),
                f.version());
        if (rows == 0) {
            throw new BusinessException(HttpStatus.CONFLICT, "STATE_CHANGED", "Phản hồi đã đổi trạng thái");
        }
        return require(f.id());
    }

    /** Mới nhất trước. */
    public List<Feedback> all() {
        return jdbc.query(SELECT + " ORDER BY submitted_at DESC, id DESC", FeedbackStore::row);
    }

    public List<Feedback> ofFamilyMember(long familyMemberId) {
        return jdbc.query(
                SELECT + " WHERE family_member_id = ? ORDER BY submitted_at DESC, id DESC",
                FeedbackStore::row,
                familyMemberId);
    }

    public List<Long> ofResident(long residentId) {
        return jdbc.queryForList("SELECT id FROM feedback WHERE resident_id = ? ORDER BY id", Long.class, residentId);
    }

    /** Chưa Đã phản hồi, đã tới hạn, chưa leo thang cho hạn hiện tại (FR-063). */
    public List<Long> dueForEscalation(Instant at) {
        return jdbc.queryForList(
                "SELECT id FROM feedback WHERE status IN " + OPEN + " AND due_at <= ? AND (escalated_due_at IS NULL"
                        + " OR escalated_due_at <> due_at) ORDER BY due_at, id",
                Long.class,
                utc(at));
    }

    /** Đã phản hồi từ trước mốc (FR-064). */
    public List<Long> repliedBefore(Instant cutoff) {
        return jdbc.queryForList(
                "SELECT id FROM feedback WHERE status = 'REPLIED' AND replied_at <= ? ORDER BY replied_at, id",
                Long.class,
                utc(cutoff));
    }

    private static Feedback row(ResultSet rs, int n) throws SQLException {
        String closeMethod = rs.getString("close_method");
        return new Feedback(
                rs.getLong("id"),
                rs.getLong("resident_id"),
                rs.getLong("family_member_id"),
                Type.valueOf(rs.getString("type")),
                ContentGroup.valueOf(rs.getString("content_group")),
                Source.valueOf(rs.getString("source")),
                instant(rs, "submitted_at"),
                Priority.valueOf(rs.getString("priority")),
                instant(rs, "due_basis_at"),
                instant(rs, "due_at"),
                AssigneeRole.valueOf(rs.getString("assignee_role")),
                rs.getObject("assignee_account_id", Long.class),
                Status.valueOf(rs.getString("status")),
                rs.getBoolean("overdue"),
                instant(rs, "escalated_due_at"),
                instant(rs, "replied_at"),
                instant(rs, "closed_at"),
                closeMethod == null ? null : CloseMethod.valueOf(closeMethod),
                rs.getLong("recorded_by"),
                rs.getInt("version"));
    }

    private static LocalDateTime utc(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    private static LocalDateTime utcOrNull(Instant instant) {
        return instant == null ? null : utc(instant);
    }

    private static Instant instant(ResultSet rs, String column) throws SQLException {
        LocalDateTime value = rs.getObject(column, LocalDateTime.class);
        return value == null ? null : value.toInstant(ZoneOffset.UTC);
    }
}
