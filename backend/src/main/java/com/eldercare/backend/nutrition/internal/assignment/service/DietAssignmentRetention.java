package com.eldercare.backend.nutrition.internal.assignment.service;

import com.eldercare.backend.administration.retention.api.RetentionDatabase;
import com.eldercare.backend.administration.retention.api.RetentionKind;
import com.eldercare.backend.administration.retention.api.spi.RetentionProvider;
import com.eldercare.backend.administration.shared.api.SharedRowRetention;
import com.eldercare.backend.resident.api.ResidentApi;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Lưu giữ bản gán chế độ ăn ({@code LuuTruProvider}; T007, research M-15): theo hồ sơ người cao tuổi (CFG-M15-03), tính
 * từ lúc người đó sang trạng thái cuối. {@link #referencing} trả bản gán của hồ sơ để hồ sơ không bị xóa trước (000 T112,
 * FR-051a); ứng viên phân trang theo {@code (at, id)}. Mẫu {@code FamilyRelationshipRetention} của 012.
 */
@Component
class DietAssignmentRetention implements RetentionProvider {

    private static final int CHUNK = 500;
    private static final Set<ResidentStatus> FINAL_STATUSES =
            Set.of(ResidentStatus.RESIDENCY_ENDED, ResidentStatus.DECEASED, ResidentStatus.ADMISSION_CANCELLED);

    private final JdbcTemplate jdbc;
    private final RetentionDatabase database;
    private final ResidentApi residents;

    DietAssignmentRetention(JdbcTemplate jdbc, RetentionDatabase database, ResidentApi residents) {
        this.jdbc = jdbc;
        this.database = database;
        this.residents = residents;
    }

    @Override
    public String dataType() {
        return DietAssignmentService.ENTITY;
    }

    @Override
    public RetentionKind retentionKind() {
        return new RetentionKind.ResidentRecord();
    }

    @Override
    public Stream<Candidate> expiredCandidates(Instant cutoff, int batch) {
        return expiredCandidates(cutoff, null, batch);
    }

    @Override
    public Stream<Candidate> expiredCandidates(Instant cutoff, Candidate after, int batch) {
        Map<Long, Instant> expired = new HashMap<>();
        for (long residentId : residents.byStatus(FINAL_STATUSES)) {
            residents
                    .finalStatusAt(residentId)
                    .filter(at -> at.isBefore(cutoff))
                    .ifPresent(at -> expired.put(residentId, at));
        }
        List<Long> ids = new ArrayList<>(expired.keySet());
        List<Candidate> candidates = new ArrayList<>();
        for (int from = 0; from < ids.size(); from += CHUNK) {
            String in = ids.subList(from, Math.min(from + CHUNK, ids.size())).stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(","));
            jdbc.query(
                    "SELECT id, resident_id FROM diet_assignment WHERE resident_id IN (" + in + ") ORDER BY id", rs -> {
                        candidates.add(new Candidate(rs.getLong(1), expired.get(rs.getLong(2))));
                    });
        }
        return RetentionProvider.page(candidates.stream(), after, batch);
    }

    /** Bản gán của hồ sơ người cao tuổi {@code target} (000 T112, FR-051a). */
    @Override
    public Set<DataRef> referencing(DataRef target) {
        if (!SharedRowRetention.RESIDENT.equals(target.dataType())) {
            return Set.of();
        }
        return jdbc
                .queryForList("SELECT id FROM diet_assignment WHERE resident_id = ?", Long.class, target.id())
                .stream()
                .map(id -> new DataRef(dataType(), id))
                .collect(Collectors.toSet());
    }

    @Override
    public Set<DataRef> referencesTo(long id) {
        return Set.of();
    }

    @Override
    public void delete(Collection<Long> ids) {
        String in = ids.stream().map(String::valueOf).collect(Collectors.joining(",", "(", ")"));
        database.jdbc().update("DELETE FROM diet_assignment WHERE id IN " + in);
    }
}
