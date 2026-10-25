package com.cambae.backend.m02.internal.application.service;

import com.cambae.backend.m01.api.ResidentApi;
import com.cambae.backend.m01.api.dto.ResidentStatus;
import com.cambae.backend.m02.internal.waitinglist.service.WaitingListService;
import com.cambae.backend.m15.retention.api.RetentionDatabase;
import com.cambae.backend.m15.retention.api.RetentionKind;
import com.cambae.backend.m15.retention.api.spi.RetentionProvider;
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
 * Lưu giữ lượt đăng ký tiếp nhận và ý kiến vào ở của nó (004 T011; FR-050, FR-051; data-model mục 3): theo hồ sơ người
 * cao tuổi (CFG-M15-03), tính từ lúc người đó sang trạng thái cuối. Ý kiến xóa cùng lượt, trước lượt. Hồ sơ chờ trỏ vào
 * lượt (user story hồ sơ chờ) khai ở {@link #referencesTo} khi có.
 */
@Component
class AdmissionApplicationRetention implements RetentionProvider {

    private static final int CHUNK = 500;
    private static final Set<ResidentStatus> FINAL_STATUSES =
            Set.of(ResidentStatus.RESIDENCY_ENDED, ResidentStatus.DECEASED, ResidentStatus.ADMISSION_CANCELLED);

    private final JdbcTemplate jdbc;
    private final RetentionDatabase database;
    private final ResidentApi residents;

    AdmissionApplicationRetention(JdbcTemplate jdbc, RetentionDatabase database, ResidentApi residents) {
        this.jdbc = jdbc;
        this.database = database;
        this.residents = residents;
    }

    @Override
    public String dataType() {
        return AdmissionApplicationService.ENTITY_TYPE;
    }

    @Override
    public RetentionKind retentionKind() {
        return new RetentionKind.ResidentRecord();
    }

    @Override
    public Stream<Candidate> expiredCandidates(Instant cutoff, int batch) {
        Map<Long, Instant> expiredResidents = new HashMap<>();
        for (long residentId : residents.byStatus(FINAL_STATUSES)) {
            residents
                    .finalStatusAt(residentId)
                    .filter(at -> at.isBefore(cutoff))
                    .ifPresent(at -> expiredResidents.put(residentId, at));
        }
        List<Long> ids = new ArrayList<>(expiredResidents.keySet());
        List<Candidate> candidates = new ArrayList<>();
        for (int from = 0; from < ids.size() && candidates.size() < batch; from += CHUNK) {
            List<Long> chunk = ids.subList(from, Math.min(from + CHUNK, ids.size()));
            String in = chunk.stream().map(String::valueOf).collect(Collectors.joining(","));
            jdbc.query(
                    "SELECT id, resident_id FROM admission_application WHERE resident_id IN (" + in + ") ORDER BY id",
                    rs -> {
                        if (candidates.size() < batch) {
                            candidates.add(new Candidate(rs.getLong(1), expiredResidents.get(rs.getLong(2))));
                        }
                    });
        }
        return candidates.stream();
    }

    /** Hồ sơ chờ của lượt (BR-M02-15): xóa cùng lượt, trước lượt vì khóa ngoại RESTRICT (FR-051a). */
    @Override
    public Set<DataRef> referencesTo(long id) {
        return Set.copyOf(jdbc.query(
                "SELECT id FROM waiting_list_entry WHERE application_id = ?",
                (rs, i) -> new DataRef(WaitingListService.ENTITY_TYPE, rs.getLong(1)),
                id));
    }

    @Override
    public void delete(Collection<Long> ids) {
        String in = ids.stream().map(String::valueOf).collect(Collectors.joining(",", "(", ")"));
        JdbcTemplate retention = database.jdbc();
        // Ý kiến cũ trỏ tới ý kiến mới qua superseded_by_id (khóa ngoại RESTRICT): xóa từ cũ tới mới
        retention.update("DELETE FROM admission_opinion WHERE application_id IN " + in + " ORDER BY id");
        retention.update("DELETE FROM admission_application WHERE id IN " + in);
    }
}
