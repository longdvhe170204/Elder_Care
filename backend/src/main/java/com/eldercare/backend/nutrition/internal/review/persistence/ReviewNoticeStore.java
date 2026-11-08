package com.eldercare.backend.nutrition.internal.review.persistence;

import com.eldercare.backend.common.data.ReminderMarkers;
import com.eldercare.backend.nutrition.api.events.ReviewDietNotice.Source;
import com.eldercare.backend.nutrition.internal.review.domain.ReviewNotice;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Thông báo xem lại chế độ ăn là mốc kỹ thuật ở {@code reminder_marker} (R-15: {@code thong_bao_xem_lai} →
 * {@code moc_da_nhac}, loại {@code M08.REVIEW_NOTICE}), không có trạng thái (Q-307): một dòng mỗi sự kiện nguồn, khóa theo
 * người cao tuổi và khóa sự kiện nên sự kiện giao lại không tạo dòng thứ hai. Bản gán chế độ ăn mới tham chiếu {@code id} của
 * dòng này (FR-059). Ghi qua {@link ReminderMarkers}; đọc bằng SQL vì bảng chung chưa có API đọc theo người.
 */
@Repository
public class ReviewNoticeStore {

    public static final String KIND = "M08.REVIEW_NOTICE";

    private static final String ENTITY_TYPE = "M08.RESIDENT";
    private static final int MAX_MARKER = 80;
    private static final int MAX_ROWS = 200;

    private final ReminderMarkers markers;
    private final JdbcTemplate jdbc;

    ReviewNoticeStore(ReminderMarkers markers, JdbcTemplate jdbc) {
        this.markers = markers;
        this.jdbc = jdbc;
    }

    /** Ghi thông báo; rỗng khi sự kiện này đã ghi (giao lại). */
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.MANDATORY)
    public Optional<Long> add(
            long residentId, Source source, long sourceId, String eventKey, Long currentAssignmentId, Instant at) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("source", source.name());
        details.put("sourceId", sourceId);
        details.put("noticedAt", at.toString());
        if (currentAssignmentId != null) {
            details.put("currentAssignmentId", currentAssignmentId);
        }
        String marker = marker(eventKey);
        if (!markers.mark(KIND, ENTITY_TYPE, residentId, marker, details)) {
            return Optional.empty();
        }
        return Optional.of(jdbc.queryForObject(
                "SELECT id FROM reminder_marker WHERE kind = ? AND entity_type = ? AND entity_id = ? AND marker = ?",
                Long.class,
                KIND,
                ENTITY_TYPE,
                residentId,
                marker));
    }

    /** Thông báo của một người, hoặc {@value #MAX_ROWS} thông báo mới nhất khi không chọn người; mới nhất trước. */
    public List<ReviewNotice> list(Long residentId) {
        List<Object> args = new ArrayList<>(List.of(KIND));
        String filter = "";
        if (residentId != null) {
            filter = " AND entity_id = ?";
            args.add(residentId);
        }
        return jdbc.query(
                "SELECT id, entity_id, details->>'$.source', CAST(details->>'$.sourceId' AS SIGNED),"
                        + " details->>'$.noticedAt', CAST(details->>'$.currentAssignmentId' AS SIGNED)"
                        + " FROM reminder_marker WHERE kind = ?" + filter + " ORDER BY id DESC LIMIT " + MAX_ROWS,
                (rs, row) -> new ReviewNotice(
                        rs.getLong(1),
                        rs.getLong(2),
                        Source.valueOf(rs.getString(3)),
                        rs.getLong(4),
                        Instant.parse(rs.getString(5)),
                        rs.getObject(6, Long.class)),
                args.toArray());
    }

    /** Thông báo {@code noticeId} có thuộc người cao tuổi này không (FR-059). */
    public boolean belongsTo(long noticeId, long residentId) {
        return Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM reminder_marker WHERE id = ? AND kind = ? AND entity_id = ?)",
                Boolean.class,
                noticeId,
                KIND,
                residentId));
    }

    /** Khóa dài hơn cột {@code marker} ({@value #MAX_MARKER}) thì dùng SHA-256 hex của khóa. */
    private static String marker(String eventKey) {
        if (eventKey.length() <= MAX_MARKER) {
            return eventKey;
        }
        try {
            return HexFormat.of()
                    .formatHex(MessageDigest.getInstance("SHA-256").digest(eventKey.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
