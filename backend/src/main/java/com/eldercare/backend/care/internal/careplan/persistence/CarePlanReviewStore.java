package com.eldercare.backend.care.internal.careplan.persistence;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.json.JsonMapper;

/**
 * Yêu cầu xem xét kế hoạch chăm sóc (FR-058, Q-448) lưu ở {@code reminder_marker}, không có bảng riêng (chủ dự án
 * 2026-11-26): mốc phát sinh {@value #RAISED} theo người cao tuổi, marker là "nguồn:mã tham chiếu" (quá 80 ký tự thì thay mã
 * tham chiếu bằng băm), {@code details} giữ nguồn, mã nguồn, mã tham chiếu. {@code marked_at} theo UTC như
 * {@code ReminderMarkers}.
 */
@Repository
public class CarePlanReviewStore {
    public static final String RAISED = "M04.CARE_PLAN_REVIEW_RAISED";
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
