package com.eldercare.backend.health.internal.governance.persistence;

import com.eldercare.backend.administration.retention.api.RetentionDatabase;
import com.eldercare.backend.administration.retention.api.RetentionKind;
import com.eldercare.backend.administration.retention.api.spi.RetentionProvider;
import com.eldercare.backend.administration.shared.api.SharedRowRetention;
import com.eldercare.backend.resident.api.ResidentApi;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Lưu giữ một bảng riêng của M06 (data-model 007 mục 14; R-11, FR-050, FR-051a; 000 T112). Bản ghi gắn hồ sơ hết hạn
 * theo lúc người cao tuổi sang trạng thái cuối (CFG-M15-03); bản ghi không gắn hồ sơ hết hạn theo {@code endedAt} khi đã
 * kết thúc (CFG-M15-04). {@code links} là bản ghi cùng chuỗi phải hết hạn và xóa cùng. Dòng bảng chung gắn theo
 * {@code sharedRowEntityType}. Chỉ tài khoản {@code eldercare_retention} xóa được (trigger chặn xóa của bảng).
 */
public class HealthTableRetention implements RetentionProvider {
    private final HealthRetentionSpec spec;
    private final JdbcTemplate jdbc;
    private final RetentionDatabase database;
    private final ObjectProvider<ResidentApi> residents;

    public HealthTableRetention(
            HealthRetentionSpec spec,
            JdbcTemplate jdbc,
            RetentionDatabase database,
            ObjectProvider<ResidentApi> residents) {
        this.spec = spec;
        this.jdbc = jdbc;
        this.database = database;
        this.residents = residents;
    }

    @Override
    public String dataType() {
        return spec.dataType();
    }

    @Override
    public String sharedRowEntityType() {
        return spec.sharedRowEntityType();
    }

    @Override
    public RetentionKind retentionKind() {
        return spec.residentLinked() ? new RetentionKind.ResidentRecord() : new RetentionKind.NotResidentLinked();
    }

    @Override
    public Stream<Candidate> expiredCandidates(Instant cutoff, int batch) {
        return expiredCandidates(cutoff, null, batch);
    }

    @Override
    public Stream<Candidate> expiredCandidates(Instant cutoff, Candidate after, int batch) {
        if (batch <= 0) return Stream.empty();
        List<Candidate> candidates = new ArrayList<>();
        if (spec.residentLinked()) {
            Map<Long, Instant> expired = ExpiredResidents.before(residents, cutoff);
            for (String in : ExpiredResidents.chunks(expired.keySet())) {
                jdbc.query(
                        "SELECT id, resident_id FROM " + spec.table() + " WHERE " + spec.where()
                                + " AND resident_id IN (" + in + ")",
                        rs -> {
                            candidates.add(new Candidate(rs.getLong(1), expired.get(rs.getLong(2))));
                        });
            }
        } else {
            jdbc.query(
                    "SELECT id, " + spec.endedAt() + " FROM " + spec.table() + " WHERE " + spec.where() + " AND "
                            + spec.endedAt() + " < ?",
                    rs -> {
                        candidates.add(
                                new Candidate(rs.getLong(1), rs.getTimestamp(2).toInstant()));
                    },
                    java.sql.Timestamp.from(cutoff));
        }
        return RetentionProvider.page(candidates.stream(), after, batch);
    }

    /** Bản ghi gắn hồ sơ người cao tuổi {@code target} (000 T112, FR-051a). */
    @Override
    public Set<DataRef> referencing(DataRef target) {
        if (!spec.residentLinked() || !SharedRowRetention.RESIDENT.equals(target.dataType())) return Set.of();
        return jdbc
                .queryForList(
                        "SELECT id FROM " + spec.table() + " WHERE " + spec.where() + " AND resident_id = ?",
                        Long.class,
                        target.id())
                .stream()
                .map(id -> new DataRef(dataType(), id))
                .collect(Collectors.toSet());
    }

    @Override
    public Set<DataRef> referencesTo(long id) {
        return spec.links().stream()
                .flatMap(link -> jdbc.queryForList(link.sql(), Long.class, id).stream()
                        .map(linked -> new DataRef(link.dataType(), linked)))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    @Override
    public void delete(Collection<Long> ids) {
        if (ids.isEmpty()) return;
        String in = ids.stream().map(String::valueOf).collect(Collectors.joining(",", "(", ")"));
        database.jdbc().update("DELETE FROM " + spec.table() + " WHERE id IN " + in);
    }
}
