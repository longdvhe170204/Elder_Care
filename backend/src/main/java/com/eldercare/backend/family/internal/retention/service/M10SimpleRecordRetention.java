package com.eldercare.backend.family.internal.retention.service;

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
 * Lưu giữ một loại {@code simple_record} của 012 có dòng bảng chung gắn vào chính nó (012 T075; research R-15 mục 2): gia
 * hạn, dấu đêm gắn {@code M10.FAMILY_STAY}; dấu nhắc gắn {@code M10.RIGHT_CHANGE_REQUEST}. {@code dataType} trùng loại
 * đối tượng của các dòng đó nên job loại bỏ xóa chúng cùng chuỗi ({@code SharedRowRetention.deleteFor}). Mốc là lúc
 * người cao tuổi sang trạng thái cuối, như quan hệ người thân (research F-17).
 */
abstract class M10SimpleRecordRetention implements RetentionProvider {

    private static final int CHUNK = 500;
    private static final Set<ResidentStatus> FINAL_STATUSES =
            Set.of(ResidentStatus.RESIDENCY_ENDED, ResidentStatus.DECEASED, ResidentStatus.ADMISSION_CANCELLED);

    private final String kind;
    private final JdbcTemplate jdbc;
    private final RetentionDatabase database;
    private final ResidentApi residents;

    M10SimpleRecordRetention(String kind, JdbcTemplate jdbc, RetentionDatabase database, ResidentApi residents) {
        this.kind = kind;
        this.jdbc = jdbc;
        this.database = database;
        this.residents = residents;
    }

    @Override
    public String dataType() {
        return kind;
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
                    "SELECT id, resident_id FROM simple_record WHERE kind = ? AND resident_id IN (" + in
                            + ") ORDER BY id",
                    rs -> {
                        candidates.add(new Candidate(rs.getLong(1), expired.get(rs.getLong(2))));
                    },
                    kind);
        }
        return RetentionProvider.page(candidates.stream(), after, batch);
    }

    /** Bản ghi gắn hồ sơ người cao tuổi {@code target} (000 T112, FR-051a). */
    @Override
    public Set<DataRef> referencing(DataRef target) {
        if (!SharedRowRetention.RESIDENT.equals(target.dataType())) {
            return Set.of();
        }
        return jdbc
                .queryForList(
                        "SELECT id FROM simple_record WHERE kind = ? AND resident_id = ?",
                        Long.class,
                        kind,
                        target.id())
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
        database.jdbc().update("DELETE FROM simple_record WHERE kind = ? AND id IN " + in, kind);
    }

    @Component
    static class FamilyStayRetention extends M10SimpleRecordRetention {

        FamilyStayRetention(JdbcTemplate jdbc, RetentionDatabase database, ResidentApi residents) {
            super("M10.FAMILY_STAY", jdbc, database, residents);
        }
    }

    @Component
    static class RightChangeRequestRetention extends M10SimpleRecordRetention {

        RightChangeRequestRetention(JdbcTemplate jdbc, RetentionDatabase database, ResidentApi residents) {
            super("M10.RIGHT_CHANGE_REQUEST", jdbc, database, residents);
        }
    }
}
