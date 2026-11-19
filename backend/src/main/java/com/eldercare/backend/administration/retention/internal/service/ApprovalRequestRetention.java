package com.eldercare.backend.administration.retention.internal.service;

import com.eldercare.backend.administration.account.api.spi.ResidencyEndApi;
import com.eldercare.backend.administration.approval.api.dto.ApprovalStatus;
import com.eldercare.backend.administration.approval.api.dto.ExtendedTransitions;
import com.eldercare.backend.administration.approval.api.spi.RequestTypeHandler;
import com.eldercare.backend.administration.parameter.api.ParameterApi;
import com.eldercare.backend.administration.parameter.api.dto.ParameterCode;
import com.eldercare.backend.administration.retention.api.RetentionDatabase;
import com.eldercare.backend.administration.retention.api.RetentionKind;
import com.eldercare.backend.administration.retention.api.spi.RetentionProvider;
import com.eldercare.backend.administration.shared.api.SharedRowRetention;
import com.eldercare.backend.common.time.BusinessTime;
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
 * mốc cuối cùng (lập, quyết định, áp dụng). Yêu cầu gắn hồ sơ người cao tuổi giữ theo hồ sơ (CFG-M15-03 từ lúc người đó
 * sang trạng thái cuối, đọc qua {@link ResidencyEndApi}; 000 T112, 2026-11-16) và đi cùng chuỗi của hồ sơ
 * ({@link #referencing}). Kết quả áp dụng (cột của yêu cầu, R-15) và bản đính chính cách C trỏ về yêu cầu: còn thì giữ
 * yêu cầu.
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
    private final ObjectProvider<ResidencyEndApi> ends;
    private final ParameterApi parameters;
    private final BusinessTime time;

    ApprovalRequestRetention(
            JdbcTemplate jdbc,
            RetentionDatabase database,
            ObjectProvider<RequestTypeHandler> handlers,
            ObjectProvider<ResidencyEndApi> ends,
            ParameterApi parameters,
            BusinessTime time) {
        this.jdbc = jdbc;
        this.database = database;
        this.handlers = handlers;
        this.ends = ends;
        this.parameters = parameters;
        this.time = time;
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
        List<Object> linkedArgs = new ArrayList<>(args);
        args.addAll(List.of(LocalDateTime.ofInstant(cutoff, ZoneOffset.UTC), afterAt, afterAt, afterId, batch));
        List<Candidate> unlinked = jdbc.query(
                "SELECT id, GREATEST(created_at, COALESCE(decided_at, created_at), COALESCE(applied_at,"
                        + " created_at)) AS at FROM approval_request WHERE " + finalStatus
                        + " AND resident_id IS NULL HAVING at < ? AND (at > ? OR (at = ? AND id > ?))"
                        + " ORDER BY at, id LIMIT ?",
                (rs, i) -> new Candidate(
                        rs.getLong("id"),
                        rs.getObject("at", LocalDateTime.class).toInstant(ZoneOffset.UTC)),
                args.toArray());
        List<Candidate> linked = residentLinked(
                "SELECT id, resident_id FROM approval_request WHERE " + finalStatus + " AND resident_id IS NOT NULL",
                linkedArgs.toArray());
        return RetentionProvider.page(Stream.concat(unlinked.stream(), linked.stream()), after, batch);
    }

    /**
     * Yêu cầu gắn hồ sơ đã hết hạn theo hồ sơ: người cao tuổi sang trạng thái cuối trước mốc CFG-M15-03 (000 T112); mốc
     * của ứng viên là lúc sang trạng thái cuối. Chưa có nguồn trạng thái cuối thì giữ.
     */
    private List<Candidate> residentLinked(String sql, Object[] args) {
        ResidencyEndApi source = ends.getIfAvailable();
        if (source == null) return List.of();
        Instant profileCutoff = time.now()
                .atZone(BusinessTime.ZONE)
                .minus(parameters.read(ParameterCode.CFG_M15_03).asPeriod())
                .toInstant();
        List<Candidate> found = new ArrayList<>();
        jdbc.query(
                sql,
                rs -> {
                    long id = rs.getLong(1);
                    source.finalStatusAt(rs.getLong(2))
                            .filter(at -> at.isBefore(profileCutoff))
                            .ifPresent(at -> found.add(new Candidate(id, at)));
                },
                args);
        return found;
    }

    /**
     * Yêu cầu ở trạng thái cuối gắn hồ sơ người cao tuổi {@code target} (000 T112, FR-051a): cùng tập với ứng viên, để
     * chuỗi của hồ sơ chỉ kéo theo yêu cầu mà job xóa được; yêu cầu còn mở không giữ hồ sơ mãi.
     */
    @Override
    public Set<DataRef> referencing(DataRef target) {
        if (!SharedRowRetention.RESIDENT.equals(target.dataType())) return Set.of();
        List<Object> args = new ArrayList<>();
        String finalStatus = finalCondition(args);
        args.add(target.id());
        return jdbc
                .queryForList(
                        "SELECT id FROM approval_request WHERE " + finalStatus + " AND resident_id = ?",
                        Long.class,
                        args.toArray())
                .stream()
                .map(id -> new DataRef(TYPE, id))
                .collect(Collectors.toSet());
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
