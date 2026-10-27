package com.cambae.backend.m02.internal.registration.service;

import com.cambae.backend.m01.api.ResidentApi;
import com.cambae.backend.m01.api.dto.ResidentStatus;
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
 * Lưu giữ khai báo ({@code LuuTruProvider}; FR-050, FR-051, NFR-07; plan 018): theo hồ sơ người cao tuổi (CFG-M15-03), tính
 * từ lúc người đó sang trạng thái cuối. Lần nộp (nhóm 3) xóa cùng khai báo, trước khai báo. Khai báo mới trỏ tới khai báo
 * cũ nó thay ({@code replaced_registration_id}) nên khai ở {@link #referencesTo} để job xóa khai báo mới trước (FR-051a).
 * Khi người không còn khai báo nào, dòng kỹ thuật của người đó (nghĩa vụ, khóa nhắc, cờ thường trú cùng xã/phường) xóa theo.
 */
@Component
class RegistrationRetention implements RetentionProvider {

    private static final int CHUNK = 500;
    private static final Set<ResidentStatus> FINAL_STATUSES =
            Set.of(ResidentStatus.RESIDENCY_ENDED, ResidentStatus.DECEASED, ResidentStatus.ADMISSION_CANCELLED);

    private final JdbcTemplate jdbc;
    private final RetentionDatabase database;
    private final ResidentApi residents;

    RegistrationRetention(JdbcTemplate jdbc, RetentionDatabase database, ResidentApi residents) {
        this.jdbc = jdbc;
        this.database = database;
        this.residents = residents;
    }

    @Override
    public String dataType() {
        return RegistrationNames.ENTITY_TYPE;
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
                    "SELECT id, resident_id FROM residence_registration WHERE resident_id IN (" + in + ") ORDER BY id",
                    rs -> {
                        if (candidates.size() < batch) {
                            candidates.add(new Candidate(rs.getLong(1), expiredResidents.get(rs.getLong(2))));
                        }
                    });
        }
        return candidates.stream();
    }

    @Override
    public Set<DataRef> referencesTo(long id) {
        return Set.copyOf(jdbc.query(
                "SELECT id FROM residence_registration WHERE replaced_registration_id = ?",
                (rs, i) -> new DataRef(RegistrationNames.ENTITY_TYPE, rs.getLong(1)),
                id));
    }

    @Override
    public void delete(Collection<Long> ids) {
        String in = ids.stream().map(String::valueOf).collect(Collectors.joining(",", "(", ")"));
        List<Long> residentIds = jdbc.queryForList(
                "SELECT DISTINCT resident_id FROM residence_registration WHERE id IN " + in, Long.class);
        JdbcTemplate retention = database.jdbc();
        retention.update("DELETE FROM registration_submission WHERE registration_id IN " + in);
        // Khai báo mới trỏ tới khai báo cũ (khóa ngoại RESTRICT): xóa từ mới tới cũ
        retention.update("DELETE FROM residence_registration WHERE id IN " + in + " ORDER BY id DESC");
        for (long residentId : residentIds) {
            // Đọc bằng cùng kết nối xóa để thấy dòng vừa xóa
            Integer left = retention.queryForObject(
                    "SELECT COUNT(*) FROM residence_registration WHERE resident_id = ?", Integer.class, residentId);
            if (left == 0) {
                retention.update("DELETE FROM registration_obligation WHERE resident_id = ?", residentId);
                retention.update("DELETE FROM registration_reminder WHERE resident_id = ?", residentId);
                retention.update("DELETE FROM registration_resident WHERE resident_id = ?", residentId);
            }
        }
    }
}
