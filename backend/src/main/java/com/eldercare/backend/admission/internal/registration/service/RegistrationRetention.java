package com.eldercare.backend.admission.internal.registration.service;

import com.eldercare.backend.administration.retention.api.RetentionDatabase;
import com.eldercare.backend.administration.retention.api.RetentionKind;
import com.eldercare.backend.administration.retention.api.spi.RetentionProvider;
import com.eldercare.backend.admission.internal.registration.domain.ObligationContent;
import com.eldercare.backend.admission.internal.registration.domain.RegistrationContent;
import com.eldercare.backend.admission.internal.registration.domain.ResidentRegistrationFlag;
import com.eldercare.backend.admission.internal.registration.domain.SubmissionContent;
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
 * Lưu giữ khai báo ({@code LuuTruProvider}; FR-050, FR-051, NFR-07; plan 018): theo hồ sơ người cao tuổi (CFG-M15-03), tính
 * từ lúc người đó sang trạng thái cuối. Khai báo là dòng {@code simple_record} loại {@code M02.RESIDENCE_REGISTRATION}
 * (R-15); lần nộp (nhóm 3, {@code record_event} cha là hồ sơ người cao tuổi, trỏ khai báo bằng {@code registrationId}) xóa
 * cùng khai báo, trước khai báo. Khai báo mới trỏ tới khai báo cũ nó thay ({@code replacedRegistrationId}) nên khai ở {@link #referencesTo} để job
 * xóa khai báo mới trước (FR-051a). Khi người không còn khai báo nào, dòng kỹ thuật của người đó (nghĩa vụ, cờ thường trú
 * cùng xã/phường ở {@code simple_record}, dấu nhắc ở {@code reminder_marker}) xóa theo.
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
                    "SELECT id, resident_id FROM simple_record WHERE kind = ? AND resident_id IN (" + in
                            + ") ORDER BY id",
                    rs -> {
                        if (candidates.size() < batch) {
                            candidates.add(new Candidate(rs.getLong(1), expiredResidents.get(rs.getLong(2))));
                        }
                    },
                    RegistrationContent.KIND.code());
        }
        return candidates.stream();
    }

    @Override
    public Set<DataRef> referencesTo(long id) {
        return Set.copyOf(jdbc.query(
                "SELECT id FROM simple_record WHERE kind = ? AND CAST(content ->> '$.replacedRegistrationId' AS UNSIGNED) = ?",
                (rs, i) -> new DataRef(RegistrationNames.ENTITY_TYPE, rs.getLong(1)),
                RegistrationContent.KIND.code(),
                id));
    }

    @Override
    public void delete(Collection<Long> ids) {
        String in = ids.stream().map(String::valueOf).collect(Collectors.joining(",", "(", ")"));
        List<Long> residentIds = jdbc.queryForList(
                "SELECT DISTINCT resident_id FROM simple_record WHERE kind = ? AND id IN " + in,
                Long.class,
                RegistrationContent.KIND.code());
        JdbcTemplate retention = database.jdbc();
        // Lần nộp (record_event, cha là hồ sơ người cao tuổi) xóa trước; khai báo xóa từ mới tới cũ
        retention.update(
                "DELETE FROM record_event WHERE kind = ? AND CAST(content ->> '$.registrationId' AS UNSIGNED) IN " + in,
                SubmissionContent.KIND.code());
        retention.update(
                "DELETE FROM simple_record WHERE kind = ? AND id IN " + in + " ORDER BY id DESC",
                RegistrationContent.KIND.code());
        for (long residentId : residentIds) {
            // Đọc bằng cùng kết nối xóa để thấy dòng vừa xóa
            Integer left = retention.queryForObject(
                    "SELECT COUNT(*) FROM simple_record WHERE kind = ? AND resident_id = ?",
                    Integer.class,
                    RegistrationContent.KIND.code(),
                    residentId);
            if (left == 0) {
                retention.update(
                        "DELETE FROM simple_record WHERE kind IN (?, ?) AND resident_id = ?",
                        ObligationContent.KIND.code(),
                        ResidentRegistrationFlag.KIND.code(),
                        residentId);
                retention.update(
                        "DELETE FROM reminder_marker WHERE kind = 'M02.REMINDER' AND entity_type LIKE 'REGISTRATION\\_%'"
                                + " AND entity_id = ?",
                        residentId);
            }
        }
    }
}
