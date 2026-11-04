package com.eldercare.backend.family.internal.relationship.service;

import com.eldercare.backend.administration.retention.api.RetentionDatabase;
import com.eldercare.backend.administration.retention.api.RetentionKind;
import com.eldercare.backend.administration.retention.api.spi.RetentionProvider;
import com.eldercare.backend.family.internal.relationship.domain.RelationshipAttribute;
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
 * Lưu giữ quan hệ người thân ({@code LuuTruProvider}; 012 T008, research F-17): theo hồ sơ người cao tuổi (CFG-M15-03),
 * tính từ lúc người đó sang trạng thái cuối. Lịch sử quyền ({@code status_history} cùng loại) do job loại bỏ xóa cùng
 * chuỗi; hồ sơ người thân xóa khi không còn quan hệ nào được giữ.
 */
@Component
class FamilyRelationshipRetention implements RetentionProvider {

    private static final int CHUNK = 500;
    private static final Set<ResidentStatus> FINAL_STATUSES =
            Set.of(ResidentStatus.RESIDENCY_ENDED, ResidentStatus.DECEASED, ResidentStatus.ADMISSION_CANCELLED);

    private final JdbcTemplate jdbc;
    private final RetentionDatabase database;
    private final ResidentApi residents;

    FamilyRelationshipRetention(JdbcTemplate jdbc, RetentionDatabase database, ResidentApi residents) {
        this.jdbc = jdbc;
        this.database = database;
        this.residents = residents;
    }

    @Override
    public String dataType() {
        return RelationshipAttribute.ENTITY_TYPE;
    }

    @Override
    public RetentionKind retentionKind() {
        return new RetentionKind.ResidentRecord();
    }

    @Override
    public Stream<Candidate> expiredCandidates(Instant cutoff, int batch) {
        Map<Long, Instant> expired = new HashMap<>();
        for (long residentId : residents.byStatus(FINAL_STATUSES)) {
            residents
                    .finalStatusAt(residentId)
                    .filter(at -> at.isBefore(cutoff))
                    .ifPresent(at -> expired.put(residentId, at));
        }
        List<Long> ids = new ArrayList<>(expired.keySet());
        List<Candidate> candidates = new ArrayList<>();
        for (int from = 0; from < ids.size() && candidates.size() < batch; from += CHUNK) {
            String in = ids.subList(from, Math.min(from + CHUNK, ids.size())).stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(","));
            jdbc.query(
                    "SELECT id, resident_id FROM family_relationship WHERE resident_id IN (" + in + ") ORDER BY id",
                    rs -> {
                        if (candidates.size() < batch) {
                            candidates.add(new Candidate(rs.getLong(1), expired.get(rs.getLong(2))));
                        }
                    });
        }
        return candidates.stream();
    }

    @Override
    public Set<DataRef> referencesTo(long id) {
        return Set.of();
    }

    @Override
    public void delete(Collection<Long> ids) {
        String in = ids.stream().map(String::valueOf).collect(Collectors.joining(",", "(", ")"));
        List<Long> members = jdbc.queryForList(
                "SELECT DISTINCT family_member_id FROM family_relationship WHERE id IN " + in, Long.class);
        JdbcTemplate retention = database.jdbc();
        retention.update("DELETE FROM family_relationship WHERE id IN " + in);
        if (!members.isEmpty()) {
            String memberIn = members.stream().map(String::valueOf).collect(Collectors.joining(",", "(", ")"));
            retention.update(
                    "DELETE FROM family_member WHERE id IN " + memberIn
                            + " AND NOT EXISTS (SELECT 1 FROM family_relationship r WHERE r.family_member_id = family_member.id)");
        }
    }
}
