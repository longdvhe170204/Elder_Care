package com.cambae.backend.m15.retention.internal;

import com.cambae.backend.m15.retention.api.RetentionDatabase;
import com.cambae.backend.m15.retention.api.RetentionKind;
import com.cambae.backend.m15.retention.api.spi.RetentionProvider;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Stream;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Lưu giữ yêu cầu phê duyệt (000 T072; FR-050, FR-051a): chỉ yêu cầu đã ở trạng thái cuối, theo CFG-M15-04 tính từ
 * mốc cuối cùng (lập, quyết định, áp dụng). Yêu cầu gắn hồ sơ người cao tuổi tính hạn từ ngày kết thúc lưu trú, chưa có
 * giao diện của 001 nên chưa hết hạn (plan 000, D5). Kết quả áp dụng ({@code application_result}) và bản đính chính
 * cách C trỏ về yêu cầu: còn thì giữ yêu cầu; dòng {@code application_result} xóa cùng yêu cầu.
 */
@Component
class ApprovalRequestRetention implements RetentionProvider {

    static final String TYPE = "M15.APPROVAL_REQUEST";

    private static final String FINAL = "('APPLIED', 'APPLY_FAILED', 'REJECTED', 'CANCELLED')";

    private final JdbcTemplate jdbc;
    private final RetentionDatabase database;

    ApprovalRequestRetention(JdbcTemplate jdbc, RetentionDatabase database) {
        this.jdbc = jdbc;
        this.database = database;
    }

    @Override
    public String dataType() {
        return TYPE;
    }

    @Override
    public RetentionKind retentionKind() {
        return new RetentionKind.NotResidentLinked();
    }

    @Override
    public Stream<Candidate> expiredCandidates(Instant cutoff, int batch) {
        return jdbc
                .query(
                        "SELECT id, GREATEST(created_at, COALESCE(decided_at, created_at), COALESCE(applied_at,"
                                + " created_at)) AS at FROM approval_request WHERE status IN " + FINAL
                                + " AND resident_id IS NULL HAVING at < ? ORDER BY at, id LIMIT ?",
                        (rs, i) -> new Candidate(
                                rs.getLong("id"),
                                rs.getObject("at", LocalDateTime.class).toInstant(ZoneOffset.UTC)),
                        LocalDateTime.ofInstant(cutoff, ZoneOffset.UTC),
                        batch)
                .stream();
    }

    /**
     * Bản ghi tác động trỏ về yêu cầu (FR-039): kết quả áp dụng, và bản đính chính cách C còn tồn tại. Kết quả là bản
     * đính chính thì chỉ tính khi bản đó còn (bản đính chính bị loại bỏ cùng bản gốc của nó).
     */
    @Override
    public Set<DataRef> referencesTo(long id) {
        Set<DataRef> refs = new LinkedHashSet<>();
        jdbc.query(
                "SELECT result_type, result_id FROM application_result WHERE approval_request_id = ?"
                        + " AND result_type <> ? AND result_id REGEXP '^[0-9]+$'",
                rs -> {
                    refs.add(new DataRef(rs.getString(1), Long.parseLong(rs.getString(2))));
                },
                id,
                CorrectionRetention.TYPE);
        jdbc.query(
                "SELECT id FROM correction WHERE approval_request_id = ?",
                rs -> {
                    refs.add(new DataRef(CorrectionRetention.TYPE, rs.getLong(1)));
                },
                id);
        return refs;
    }

    @Override
    public void delete(Collection<Long> ids) {
        String in = Ids.inList(ids);
        database.jdbc().update("DELETE FROM application_result WHERE approval_request_id IN (" + in + ")");
        database.jdbc().update("DELETE FROM approval_request WHERE id IN (" + in + ")");
    }
}
