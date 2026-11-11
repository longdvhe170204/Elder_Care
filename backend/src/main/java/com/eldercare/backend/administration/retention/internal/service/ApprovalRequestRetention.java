package com.eldercare.backend.administration.retention.internal.service;

import com.eldercare.backend.administration.approval.api.dto.ApprovalStatus;
import com.eldercare.backend.administration.approval.api.dto.ExtendedTransitions;
import com.eldercare.backend.administration.approval.api.spi.RequestTypeHandler;
import com.eldercare.backend.administration.retention.api.RetentionDatabase;
import com.eldercare.backend.administration.retention.api.RetentionKind;
import com.eldercare.backend.administration.retention.api.spi.RetentionProvider;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Lưu giữ yêu cầu phê duyệt (000 T072; FR-050, FR-051a): chỉ yêu cầu đã ở trạng thái cuối, theo CFG-M15-04 tính từ
 * mốc cuối cùng (lập, quyết định, áp dụng). Yêu cầu gắn hồ sơ người cao tuổi tính hạn từ ngày kết thúc lưu trú, chưa có
 * giao diện của 001 nên chưa hết hạn (plan 000, D5). Kết quả áp dụng (cột của yêu cầu, R-15) và bản đính chính
 * cách C trỏ về yêu cầu: còn thì giữ yêu cầu.
 */
@Component
class ApprovalRequestRetention implements RetentionProvider {

    static final String TYPE = "M15.APPROVAL_REQUEST";

    private static final String FINAL = "('APPLIED', 'APPLY_FAILED', 'REJECTED', 'CANCELLED')";

    /** Trạng thái chung còn mở: chuyển riêng của một loại có thể quay về đây, nên không phải trạng thái cuối. */
    private static final Set<String> OPEN =
            Set.of(ApprovalStatus.DRAFT, ApprovalStatus.PENDING_APPROVAL, ApprovalStatus.APPROVED);

    private final JdbcTemplate jdbc;
    private final RetentionDatabase database;
    private final ObjectProvider<RequestTypeHandler> handlers;

    ApprovalRequestRetention(
            JdbcTemplate jdbc, RetentionDatabase database, ObjectProvider<RequestTypeHandler> handlers) {
        this.jdbc = jdbc;
        this.database = database;
        this.handlers = handlers;
    }

    /**
     * Trạng thái cuối riêng của một loại (000 T114; 015 FR-046): đích của chuyển trạng thái riêng mà không là nguồn của
     * chuyển riêng nào và không phải trạng thái chung còn mở, ví dụ {@code RECIPIENT_DECLINED}, {@code EXPIRED},
     * {@code PARTIALLY_APPLIED} của 015, {@code SUPERSEDED} của 004.
     */
    static Set<String> extendedFinalStatuses(ExtendedTransitions declared) {
        Set<String> sources = declared.byCode().values().stream()
                .flatMap(t -> t.from().stream())
                .collect(Collectors.toSet());
        return declared.byCode().values().stream()
                .map(ExtendedTransitions.Transition::to)
                .filter(to -> !sources.contains(to) && !OPEN.contains(to))
                .collect(Collectors.toCollection(TreeSet::new));
    }

    /** Điều kiện trạng thái cuối: trạng thái chung, hoặc trạng thái cuối riêng của đúng loại của yêu cầu. */
    private String finalCondition(List<Object> args) {
        StringBuilder sql = new StringBuilder("(status IN " + FINAL);
        handlers.orderedStream().forEach(handler -> {
            Set<String> statuses = extendedFinalStatuses(handler.extendedTransitions());
            if (!statuses.isEmpty()) {
                sql.append(" OR (type = ? AND status IN (")
                        .append(String.join(", ", Collections.nCopies(statuses.size(), "?")))
                        .append("))");
                args.add(handler.typeCode());
                args.addAll(statuses);
            }
        });
        return sql.append(')').toString();
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
        return expiredCandidates(cutoff, null, batch);
    }

    /** Trang sau mốc {@code (at, id)} (000 T112). */
    @Override
    public Stream<Candidate> expiredCandidates(Instant cutoff, Candidate after, int batch) {
        LocalDateTime afterAt = after == null
                ? LocalDateTime.of(1000, 1, 1, 0, 0)
                : LocalDateTime.ofInstant(after.at(), ZoneOffset.UTC);
        long afterId = after == null ? 0 : after.id();
        List<Object> args = new ArrayList<>();
        String finalStatus = finalCondition(args);
        args.addAll(List.of(LocalDateTime.ofInstant(cutoff, ZoneOffset.UTC), afterAt, afterAt, afterId, batch));
        return jdbc
                .query(
                        "SELECT id, GREATEST(created_at, COALESCE(decided_at, created_at), COALESCE(applied_at,"
                                + " created_at)) AS at FROM approval_request WHERE " + finalStatus
                                + " AND resident_id IS NULL HAVING at < ? AND (at > ? OR (at = ? AND id > ?))"
                                + " ORDER BY at, id LIMIT ?",
                        (rs, i) -> new Candidate(
                                rs.getLong("id"),
                                rs.getObject("at", LocalDateTime.class).toInstant(ZoneOffset.UTC)),
                        args.toArray())
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
                "SELECT result_type, result_id FROM approval_request WHERE id = ? AND result_type IS NOT NULL"
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
        database.jdbc().update("DELETE FROM approval_request WHERE id IN (" + in + ")");
    }
}
