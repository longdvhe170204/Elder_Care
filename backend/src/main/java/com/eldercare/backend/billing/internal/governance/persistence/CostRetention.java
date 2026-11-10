package com.eldercare.backend.billing.internal.governance.persistence;

import com.eldercare.backend.administration.retention.api.RetentionDatabase;
import com.eldercare.backend.administration.retention.api.RetentionKind;
import com.eldercare.backend.administration.retention.api.spi.RetentionProvider;
import com.eldercare.backend.resident.api.ResidentApi;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.springframework.jdbc.core.JdbcTemplate;

/** Mốc lưu giữ và tham chiếu thật của ba loại dữ liệu chi phí đã có (data-model mục 10). */
@org.springframework.stereotype.Repository
public class CostRetention {
    public enum Kind {
        PERIOD,
        STATEMENT,
        ITEM,
        /** Chỉ mục nguồn (dữ liệu kỹ thuật, T033): đi theo hồ sơ người cao tuổi, không có bản trỏ tới. */
        SOURCE_INDEX
    }

    private static final Set<ResidentStatus> FINAL_STATUSES =
            Set.of(ResidentStatus.RESIDENCY_ENDED, ResidentStatus.DECEASED, ResidentStatus.ADMISSION_CANCELLED);
    private final JdbcTemplate jdbc;
    private final RetentionDatabase retention;
    private final ResidentApi residents;

    public CostRetention(JdbcTemplate jdbc, RetentionDatabase retention, ResidentApi residents) {
        this.jdbc = jdbc;
        this.retention = retention;
        this.residents = residents;
    }

    public RetentionProvider provider(Kind kind) {
        return new Provider(kind);
    }

    private final class Provider implements RetentionProvider {
        private final Kind kind;

        private Provider(Kind kind) {
            this.kind = kind;
        }

        @Override
        public String dataType() {
            return "M11.COST_" + kind.name();
        }

        @Override
        public RetentionKind retentionKind() {
            return kind == Kind.PERIOD ? new RetentionKind.NotResidentLinked() : new RetentionKind.ResidentRecord();
        }

        @Override
        public Stream<Candidate> expiredCandidates(Instant cutoff, int batch) {
            return expiredCandidates(cutoff, null, batch);
        }

        @Override
        public Stream<Candidate> expiredCandidates(Instant cutoff, Candidate after, int batch) {
            if (batch <= 0) return Stream.empty();
            if (kind == Kind.PERIOD) {
                return jdbc
                        .query(
                                "SELECT id,created_at FROM cost_statement WHERE kind='PERIOD' AND created_at<?"
                                        + " AND (created_at>? OR (created_at=? AND id>?)) ORDER BY created_at,id LIMIT ?",
                                (rs, row) -> new Candidate(
                                        rs.getLong("id"),
                                        rs.getTimestamp("created_at").toInstant()),
                                Timestamp.from(cutoff),
                                Timestamp.from(after == null ? Instant.parse("1000-01-01T00:00:00Z") : after.at()),
                                Timestamp.from(after == null ? Instant.parse("1000-01-01T00:00:00Z") : after.at()),
                                after == null ? 0 : after.id(),
                                batch)
                        .stream();
            }
            List<Candidate> milestones = new ArrayList<>();
            for (long residentId : residents.byStatus(FINAL_STATUSES)) {
                residents
                        .finalStatusAt(residentId)
                        .filter(at -> at.isBefore(cutoff))
                        .ifPresent(at -> milestones.add(new Candidate(residentId, at)));
            }
            milestones.sort(Comparator.comparing(Candidate::at).thenComparingLong(Candidate::id));
            String query = switch (kind) {
                case STATEMENT ->
                    "SELECT id FROM cost_statement WHERE kind='STATEMENT' AND resident_id=? AND id>? ORDER BY id LIMIT ?";
                case SOURCE_INDEX ->
                    "SELECT id FROM cost_source_index WHERE resident_id=? AND id>? ORDER BY id LIMIT ?";
                default -> "SELECT id FROM cost_item WHERE resident_id=? AND id>? ORDER BY id LIMIT ?";
            };
            List<Candidate> result = new ArrayList<>();
            for (Candidate milestone : milestones) {
                if (after != null && milestone.at().isBefore(after.at())) continue;
                long afterId = after != null && milestone.at().equals(after.at()) ? after.id() : 0;
                for (long id : jdbc.queryForList(query, Long.class, milestone.id(), afterId, batch)) {
                    result.add(new Candidate(id, milestone.at()));
                }
            }
            return RetentionProvider.page(result.stream(), after, batch);
        }

        @Override
        public Set<DataRef> referencing(DataRef target) {
            if (kind == Kind.PERIOD || !target.dataType().equals("M01.RESIDENT")) return Set.of();
            String sql = switch (kind) {
                case STATEMENT -> "SELECT id FROM cost_statement WHERE kind='STATEMENT' AND resident_id=?";
                case SOURCE_INDEX -> "SELECT id FROM cost_source_index WHERE resident_id=?";
                default -> "SELECT id FROM cost_item WHERE resident_id=?";
            };
            Set<DataRef> references = new LinkedHashSet<>();
            jdbc.queryForList(sql, Long.class, target.id()).forEach(id -> references.add(new DataRef(dataType(), id)));
            return references;
        }

        @Override
        public Set<DataRef> referencesTo(long id) {
            Set<DataRef> references = new LinkedHashSet<>();
            if (kind == Kind.SOURCE_INDEX) return references;
            if (kind != Kind.ITEM
                    && jdbc.queryForObject(
                                    "SELECT COUNT(*) FROM cost_statement WHERE id=? AND kind=?",
                                    Long.class,
                                    id,
                                    kind.name())
                            == 0) {
                return references;
            }
            if (kind == Kind.PERIOD) {
                jdbc.queryForList(
                                "SELECT id FROM cost_statement WHERE kind='STATEMENT' AND period_id=?", Long.class, id)
                        .forEach(child -> references.add(new DataRef("M11.COST_STATEMENT", child)));
                jdbc.queryForList(
                                "SELECT id FROM cost_item WHERE period_id=? OR original_period_id=?",
                                Long.class,
                                id,
                                id)
                        .forEach(child -> references.add(new DataRef("M11.COST_ITEM", child)));
            } else {
                String query = kind == Kind.STATEMENT
                        ? "SELECT id FROM cost_item WHERE statement_id=?"
                        : "SELECT id FROM cost_item WHERE original_item_id=?";
                jdbc.queryForList(query, Long.class, id)
                        .forEach(child -> references.add(new DataRef("M11.COST_ITEM", child)));
            }
            return references;
        }

        @Override
        public void delete(Collection<Long> ids) {
            String query = switch (kind) {
                case ITEM -> "DELETE FROM cost_item WHERE id=?";
                case SOURCE_INDEX -> "DELETE FROM cost_source_index WHERE id=?";
                default -> "DELETE FROM cost_statement WHERE id=? AND kind=?";
            };
            // Job đã xếp bản trỏ tới trước bản gốc; không sắp lại ID làm đảo thứ tự khóa ngoại.
            for (long id : ids.stream().distinct().toList()) {
                if (kind == Kind.ITEM || kind == Kind.SOURCE_INDEX)
                    retention.jdbc().update(query, id);
                else retention.jdbc().update(query, id, kind.name());
            }
        }
    }
}
