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
        ITEM
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
            if (batch <= 0) return Stream.empty();
            if (kind == Kind.PERIOD) {
                return jdbc
                        .query(
                                "SELECT id,created_at FROM cost_statement WHERE kind='PERIOD' AND created_at<? ORDER BY created_at,id LIMIT ?",
                                (rs, row) -> new Candidate(
                                        rs.getLong("id"),
                                        rs.getTimestamp("created_at").toInstant()),
                                Timestamp.from(cutoff),
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
            String query = kind == Kind.STATEMENT
                    ? "SELECT id FROM cost_statement WHERE kind='STATEMENT' AND resident_id=? ORDER BY id LIMIT ?"
                    : "SELECT id FROM cost_item WHERE resident_id=? ORDER BY id LIMIT ?";
            List<Candidate> result = new ArrayList<>();
            for (Candidate milestone : milestones) {
                if (result.size() == batch) break;
                for (long id : jdbc.queryForList(query, Long.class, milestone.id(), batch - result.size())) {
                    result.add(new Candidate(id, milestone.at()));
                }
            }
            return result.stream();
        }

        @Override
        public Set<DataRef> referencesTo(long id) {
            Set<DataRef> references = new LinkedHashSet<>();
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
            String query = kind == Kind.ITEM
                    ? "DELETE FROM cost_item WHERE id=?"
                    : "DELETE FROM cost_statement WHERE id=? AND kind=?";
            // Job đã xếp bản trỏ tới trước bản gốc; không sắp lại ID làm đảo thứ tự khóa ngoại.
            for (long id : ids.stream().distinct().toList()) {
                if (kind == Kind.ITEM) retention.jdbc().update(query, id);
                else retention.jdbc().update(query, id, kind.name());
            }
        }
    }
}
