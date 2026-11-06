package com.eldercare.backend.family.internal.pickup.service;

import com.eldercare.backend.administration.retention.api.RetentionDatabase;
import com.eldercare.backend.administration.retention.api.RetentionKind;
import com.eldercare.backend.administration.retention.api.spi.RetentionProvider;
import com.eldercare.backend.administration.shared.api.SharedRowRetention;
import com.eldercare.backend.family.internal.pickup.domain.PickupRecord;
import com.eldercare.backend.resident.api.ResidentApi;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Lưu giữ bản ghi đón ({@code LuuTruProvider}; research F-17): theo hồ sơ người cao tuổi (CFG-M15-03), tính từ lúc người
 * đó sang trạng thái cuối. Sự kiện kết quả gắn {@code M10.PICKUP_RECORD} do job xóa cùng chuỗi.
 */
@Component
class PickupRecordRetention implements RetentionProvider {

    private static final Set<ResidentStatus> FINAL_STATUSES =
            Set.of(ResidentStatus.RESIDENCY_ENDED, ResidentStatus.DECEASED, ResidentStatus.ADMISSION_CANCELLED);

    private final JdbcTemplate jdbc;
    private final RetentionDatabase database;
    private final ResidentApi residents;

    PickupRecordRetention(JdbcTemplate jdbc, RetentionDatabase database, ResidentApi residents) {
        this.jdbc = jdbc;
        this.database = database;
        this.residents = residents;
    }

    @Override
    public String dataType() {
        return PickupRecord.ENTITY_TYPE;
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
        List<Candidate> candidates = new ArrayList<>();
        for (long residentId : residents.byStatus(FINAL_STATUSES)) {
            residents
                    .finalStatusAt(residentId)
                    .filter(at -> at.isBefore(cutoff))
                    .ifPresent(at -> jdbc.query(
                            "SELECT id FROM pickup_record WHERE resident_id = ? ORDER BY id",
                            rs -> {
                                candidates.add(new Candidate(rs.getLong(1), at));
                            },
                            residentId));
        }
        return RetentionProvider.page(candidates.stream(), after, batch);
    }

    /** Bản ghi gắn hồ sơ người cao tuổi {@code target} (000 T112, FR-051a). */
    @Override
    public Set<DataRef> referencing(DataRef target) {
        if (!SharedRowRetention.RESIDENT.equals(target.dataType())) {
            return Set.of();
        }
        return jdbc.queryForList("SELECT id FROM pickup_record WHERE resident_id = ?", Long.class, target.id()).stream()
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
        database.jdbc().update("DELETE FROM pickup_record WHERE id IN " + in);
    }
}
