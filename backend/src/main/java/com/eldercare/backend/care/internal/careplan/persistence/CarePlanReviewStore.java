package com.eldercare.backend.care.internal.careplan.persistence;

import com.eldercare.backend.care.internal.careplan.domain.CarePlanReviewRaised;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.json.JsonMapper;

/**
 * Yêu cầu xem xét kế hoạch chăm sóc (FR-058, Q-448) lưu ở {@code reminder_marker}, không có bảng riêng (chủ dự án
 * 2026-11-26): mốc phát sinh {@value #RAISED} theo người cao tuổi, marker là "nguồn:mã tham chiếu" (quá 80 ký tự thì thay mã
 * tham chiếu bằng băm), {@code details} giữ nguồn, mã nguồn, mã tham chiếu; mốc "Không cần thay đổi" {@value #DISMISSED},
 * marker là thời điểm, {@code details} giữ tài khoản và lý do. {@code marked_at} theo UTC như {@code ReminderMarkers}.
 */
@Repository
public class CarePlanReviewStore {
    public static final String RAISED = "M04.CARE_PLAN_REVIEW_RAISED";
    public static final String DISMISSED = "M04.CARE_PLAN_REVIEW_DISMISSED";
    static final String RESIDENT = "M01.RESIDENT";
    private static final int MARKER_LENGTH = 80;

    private final JdbcTemplate jdbc;
    private final JsonMapper json;

    public CarePlanReviewStore(JdbcTemplate jdbc, JsonMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    /** Ghi mốc phát sinh; cùng người cao tuổi, nguồn và mã tham chiếu đã có thì bỏ qua. */
    public void raise(long residentId, String source, long sourceId, String reference, Instant raisedAt) {
        jdbc.update(
                "INSERT IGNORE INTO reminder_marker (kind, entity_type, entity_id, marker, marked_at, details)"
                        + " VALUES (?, ?, ?, ?, ?, ?)",
                RAISED,
                RESIDENT,
                residentId,
                marker(source, reference),
                LocalDateTime.ofInstant(raisedAt, ZoneOffset.UTC),
                json.writeValueAsString(Map.of("source", source, "sourceId", sourceId, "reference", reference)));
    }

    /** Các mốc phát sinh của người cao tuổi, cũ trước. */
    public List<CarePlanReviewRaised> raised(long residentId) {
        return jdbc.query(
                "SELECT marked_at, details FROM reminder_marker WHERE kind = ? AND entity_type = ? AND entity_id = ?"
                        + " ORDER BY marked_at, id",
                (rs, i) -> {
                    var details = json.readTree(rs.getString("details"));
                    return new CarePlanReviewRaised(
                            details.get("source").asString(),
                            details.get("sourceId").asLong(),
                            details.get("reference").asString(),
                            utc(rs.getObject("marked_at", LocalDateTime.class)));
                },
                RAISED,
                RESIDENT,
                residentId);
    }

    /** Các lần "Không cần thay đổi" của người cao tuổi. */
    public List<Instant> dismissals(long residentId) {
        return jdbc.query(
                "SELECT marked_at FROM reminder_marker WHERE kind = ? AND entity_type = ? AND entity_id = ?",
                (rs, i) -> utc(rs.getObject("marked_at", LocalDateTime.class)),
                DISMISSED,
                RESIDENT,
                residentId);
    }

    /** Ghi lần "Không cần thay đổi" (FR-058); marker là thời điểm nên mỗi lần một mốc. */
    public void dismiss(long residentId, long accountId, String reason, Instant at) {
        jdbc.update(
                "INSERT INTO reminder_marker (kind, entity_type, entity_id, marker, marked_at, details)"
                        + " VALUES (?, ?, ?, ?, ?, ?)",
                DISMISSED,
                RESIDENT,
                residentId,
                at.toString(),
                LocalDateTime.ofInstant(at, ZoneOffset.UTC),
                json.writeValueAsString(Map.of("accountId", accountId, "reason", reason)));
    }

    private static Instant utc(LocalDateTime value) {
        return value.toInstant(ZoneOffset.UTC);
    }

    static String marker(String source, String reference) {
        String marker = source + ":" + reference;
        if (marker.length() <= MARKER_LENGTH) return marker;
        return (source + ":" + sha256(reference)).substring(0, MARKER_LENGTH);
    }

    private static String sha256(String value) {
        try {
            return java.util.HexFormat.of()
                    .formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("JDK thiếu SHA-256", e);
        }
    }
}
