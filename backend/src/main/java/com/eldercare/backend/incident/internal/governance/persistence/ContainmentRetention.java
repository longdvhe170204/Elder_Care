package com.eldercare.backend.incident.internal.governance.persistence;

import com.eldercare.backend.administration.retention.api.RetentionDatabase;
import com.eldercare.backend.administration.retention.api.RetentionKind;
import com.eldercare.backend.administration.retention.api.spi.RetentionProvider;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Lưu giữ khoanh vùng ({@code simple_record} {@code M05.CONTAINMENT}; data-model 007 mục 14): không gắn hồ sơ
 * (CFG-M15-04) tính từ lúc gỡ; vùng chưa gỡ không hết hạn. Sự cố gắn vùng cùng chuỗi, nên vùng chỉ xóa khi các sự cố đó
 * cũng hết hạn và ngược lại.
 */
public class ContainmentRetention implements RetentionProvider {
    public static final String TYPE = "M05.CONTAINMENT";

    private final JdbcTemplate jdbc;
    private final RetentionDatabase database;

    public ContainmentRetention(JdbcTemplate jdbc, RetentionDatabase database) {
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
        return expiredCandidates(cutoff, null, batch);
    }

    @Override
    public Stream<Candidate> expiredCandidates(Instant cutoff, Candidate after, int batch) {
        if (batch <= 0) return Stream.empty();
        List<Candidate> candidates = new ArrayList<>();
        jdbc.query(
                "SELECT id, JSON_UNQUOTE(JSON_EXTRACT(content, '$.period.releasedAt')) FROM simple_record"
                        + " WHERE kind = ? AND status = 'RELEASED'",
                rs -> {
                    Instant released = instant(rs.getString(2));
                    if (released != null && released.isBefore(cutoff)) {
                        candidates.add(new Candidate(rs.getLong(1), released));
                    }
                },
                TYPE);
        return RetentionProvider.page(candidates.stream(), after, batch);
    }

    @Override
    public Set<DataRef> referencesTo(long id) {
        return jdbc
                .query(
                        "SELECT i.id, i.resident_id FROM simple_record c"
                                + " JOIN incident i ON JSON_CONTAINS(JSON_EXTRACT(c.content, '$.incidentIds'), CAST(i.id AS JSON))"
                                + " WHERE c.kind = ? AND c.id = ?",
                        (rs, n) -> {
                            rs.getLong(2);
                            String type = rs.wasNull()
                                    ? IncidentRetentionTypes.FACILITY_INCIDENT
                                    : IncidentRetentionTypes.INCIDENT;
                            return new DataRef(type, rs.getLong(1));
                        },
                        TYPE,
                        id)
                .stream()
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    @Override
    public void delete(Collection<Long> ids) {
        if (ids.isEmpty()) return;
        String in = ids.stream().map(String::valueOf).collect(Collectors.joining(",", "(", ")"));
        database.jdbc().update("DELETE FROM simple_record WHERE kind = ? AND id IN " + in, TYPE);
    }

    /** Thời điểm trong JSON: chuỗi ISO-8601 hoặc số giây kể từ epoch (tùy cấu hình Jackson). */
    private static Instant instant(String value) {
        if (value == null || value.isBlank() || "null".equals(value)) return null;
        try {
            return Instant.parse(value);
        } catch (java.time.format.DateTimeParseException e) {
            BigDecimal seconds = new BigDecimal(value);
            return Instant.ofEpochSecond(
                    seconds.longValue(),
                    seconds.remainder(BigDecimal.ONE).movePointRight(9).longValue());
        }
    }
}
