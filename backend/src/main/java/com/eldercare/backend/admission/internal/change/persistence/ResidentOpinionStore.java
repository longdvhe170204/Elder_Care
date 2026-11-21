package com.eldercare.backend.admission.internal.change.persistence;

import com.eldercare.backend.admission.internal.change.domain.ResidentOpinion;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Ý kiến người cao tuổi về yêu cầu thay đổi lưu trú ({@code residency_change_resident_opinion}, nhóm 3, chỉ thêm; FR-043h)
 * và dấu nội dung lúc người đại diện gửi qua cổng ({@code residency_change_portal_digest}, ghi một lần; FR-043i).
 */
@Repository
public class ResidentOpinionStore {

    private final JdbcTemplate jdbc;

    ResidentOpinionStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void insert(
            long approvalRequestId,
            long residentId,
            ResidentOpinion.Opinion opinion,
            ResidentOpinion.Source source,
            String note,
            String contentDigest,
            long recordedBy,
            Instant at) {
        jdbc.update(
                "INSERT INTO residency_change_resident_opinion (approval_request_id, resident_id, opinion, source, note,"
                        + " content_digest, recorded_by, recorded_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                approvalRequestId,
                residentId,
                opinion.name(),
                source.name(),
                note,
                contentDigest,
                recordedBy,
                LocalDateTime.ofInstant(at, ZoneOffset.UTC));
    }

    /** Mọi ý kiến của yêu cầu, mới nhất trước. */
    public List<ResidentOpinion> ofRequest(long approvalRequestId) {
        return jdbc.query(
                "SELECT * FROM residency_change_resident_opinion WHERE approval_request_id = ?"
                        + " ORDER BY recorded_at DESC, id DESC",
                (rs, i) -> map(rs),
                approvalRequestId);
    }

    /** Ý kiến hiện hành (mới nhất). */
    public Optional<ResidentOpinion> current(long approvalRequestId) {
        return ofRequest(approvalRequestId).stream().findFirst();
    }

    /** Dấu nội dung lúc người đại diện gửi qua cổng; ghi lặp thì giữ dấu đầu. */
    public void savePortalDigest(long approvalRequestId, String digest, Instant at) {
        jdbc.update(
                "INSERT IGNORE INTO residency_change_portal_digest (approval_request_id, digest, created_at)"
                        + " VALUES (?, ?, ?)",
                approvalRequestId,
                digest,
                LocalDateTime.ofInstant(at, ZoneOffset.UTC));
    }

    public Optional<String> portalDigest(long approvalRequestId) {
        return jdbc
                .queryForList(
                        "SELECT digest FROM residency_change_portal_digest WHERE approval_request_id = ?",
                        String.class,
                        approvalRequestId)
                .stream()
                .findFirst();
    }

    private static ResidentOpinion map(ResultSet rs) throws SQLException {
        return new ResidentOpinion(
                rs.getLong("id"),
                rs.getLong("approval_request_id"),
                rs.getLong("resident_id"),
                ResidentOpinion.Opinion.valueOf(rs.getString("opinion")),
                ResidentOpinion.Source.valueOf(rs.getString("source")),
                rs.getString("note"),
                rs.getString("content_digest"),
                rs.getLong("recorded_by"),
                rs.getObject("recorded_at", LocalDateTime.class).toInstant(ZoneOffset.UTC));
    }
}
