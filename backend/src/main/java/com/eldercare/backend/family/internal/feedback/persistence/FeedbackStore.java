package com.eldercare.backend.family.internal.feedback.persistence;

import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.family.internal.feedback.domain.Feedback;
import com.eldercare.backend.family.internal.feedback.domain.Feedback.AssigneeRole;
import com.eldercare.backend.family.internal.feedback.domain.Feedback.CloseMethod;
import com.eldercare.backend.family.internal.feedback.domain.Feedback.ContentGroup;
import com.eldercare.backend.family.internal.feedback.domain.Feedback.Priority;
import com.eldercare.backend.family.internal.feedback.domain.Feedback.SenderType;
import com.eldercare.backend.family.internal.feedback.domain.Feedback.Source;
import com.eldercare.backend.family.internal.feedback.domain.Feedback.Status;
import com.eldercare.backend.family.internal.feedback.domain.Feedback.Type;
import com.eldercare.backend.family.internal.feedback.domain.Feedback.WishTopic;
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

/** Đọc, ghi {@code feedback} (V012_07, V012_10; nhóm 2); đổi trạng thái kiểm {@code version}. */
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
                            "INSERT INTO feedback (resident_id, sender_type, family_member_id, recorded_on_behalf_by,"
                                    + " type, content_group, wish_topic, source, submitted_at, priority, due_basis_at,"
                                    + " due_at, assignee_role, assignee_account_id, status, recorded_by, created_at)"
                                    + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                            Statement.RETURN_GENERATED_KEYS);
                    s.setLong(1, f.residentId());
                    s.setString(2, f.senderType().name());
                    s.setObject(3, f.familyMemberId(), Types.BIGINT);
                    s.setObject(4, f.recordedOnBehalfBy(), Types.BIGINT);
                    s.setString(5, f.type().name());
                    s.setString(6, f.group().name());
                    s.setString(7, f.wishTopic() == null ? null : f.wishTopic().name());
                    s.setString(8, f.source().name());
                    s.setObject(9, utc(f.submittedAt()));
                    s.setString(10, f.priority().name());
                    s.setObject(11, utc(f.dueBasisAt()));
                    s.setObject(12, utc(f.dueAt()));
                    s.setString(13, f.assigneeRole().name());
                    s.setObject(14, f.assigneeAccountId(), Types.BIGINT);
                    s.setString(15, f.status().name());
                    s.setLong(16, f.recordedBy());
                    s.setObject(17, utc(createdAt));
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

    /** Ghi loại, trạng thái, mức, hạn, người phụ trách; 409 {@code STATE_CHANGED} khi phiên bản đã đổi. */
    public Feedback update(Feedback f) {
        int rows = jdbc.update(
                "UPDATE feedback SET type = ?, wish_topic = ?, priority = ?, due_basis_at = ?, due_at = ?,"
                        + " assignee_role = ?,"
                        + " assignee_account_id = ?, status = ?, overdue = ?, escalated_due_at = ?, replied_at = ?,"
                        + " closed_at = ?, close_method = ?, version = version + 1 WHERE id = ? AND version = ?",
                f.type().name(),
                f.wishTopic() == null ? null : f.wishTopic().name(),
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

    /** Phản hồi người thân gửi (FR-086: không gồm phản hồi của người cao tuổi). */
    public List<Feedback> ofFamilyMember(long familyMemberId) {
        return jdbc.query(
                SELECT + " WHERE family_member_id = ? AND sender_type = 'FAMILY_MEMBER'"
                        + " ORDER BY submitted_at DESC, id DESC",
                FeedbackStore::row,
                familyMemberId);
    }

    /** Phản hồi của chính người cao tuổi, kể cả được ghi thay (FR-058a, FR-086). */
    public List<Feedback> fromResident(long residentId) {
        return jdbc.query(
                SELECT + " WHERE resident_id = ? AND sender_type = 'RESIDENT' ORDER BY submitted_at DESC, id DESC",
                FeedbackStore::row,
                residentId);
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
        String wishTopic = rs.getString("wish_topic");
        return new Feedback(
                rs.getLong("id"),
                rs.getLong("resident_id"),
                SenderType.valueOf(rs.getString("sender_type")),
                rs.getObject("family_member_id", Long.class),
                rs.getObject("recorded_on_behalf_by", Long.class),
                Type.valueOf(rs.getString("type")),
                ContentGroup.valueOf(rs.getString("content_group")),
                wishTopic == null ? null : WishTopic.valueOf(wishTopic),
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
