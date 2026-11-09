package com.eldercare.backend.medication.internal.governance.persistence;

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
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Lưu giữ dữ liệu thuốc của người cao tuổi (data-model 006 mục 13, research R-11; 006 F-06): một provider cho mỗi bảng
 * hoặc loại dòng, theo hồ sơ (CFG-M15-03) tính từ lúc người đó sang trạng thái cuối. {@link #referencesTo} trả bản ghi
 * trỏ tới (liều của đơn, kết quả của liều, đơn thay thế, lần nhận lại của lần giao) để job xóa bản trỏ tới trước;
 * {@link #referencing} trả bản ghi của hồ sơ để hồ sơ không bị xóa trước (000 T112, FR-051a). Danh mục thuốc (nhóm 1)
 * không bị loại bỏ. Các bean tạo ở {@code MedicationConfig}.
 */
public class MedicationRetentionProvider implements RetentionProvider {

    public static final String TYPE = "M07.PRESCRIPTION";

    private static final int CHUNK = 500;
    private static final Set<ResidentStatus> FINAL_STATUSES =
            Set.of(ResidentStatus.RESIDENCY_ENDED, ResidentStatus.DECEASED, ResidentStatus.ADMISSION_CANCELLED);

    private final RetentionSpec spec;
    private final JdbcTemplate jdbc;
    private final RetentionDatabase database;
    private final ObjectProvider<ResidentApi> residents;

    public MedicationRetentionProvider(
            RetentionSpec spec, JdbcTemplate jdbc, RetentionDatabase database, ObjectProvider<ResidentApi> residents) {
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
    public RetentionKind retentionKind() {
        return new RetentionKind.ResidentRecord();
    }

    @Override
    public Stream<Candidate> expiredCandidates(Instant cutoff, int batch) {
        return expiredCandidates(cutoff, null, batch);
    }

    @Override
    public Stream<Candidate> expiredCandidates(Instant cutoff, Candidate after, int batch) {
        ResidentApi residentApi = residents.getIfAvailable();
        if (batch <= 0 || residentApi == null) {
            return Stream.empty();
        }
        Map<Long, Instant> expired = new HashMap<>();
        for (long residentId : residentApi.byStatus(FINAL_STATUSES)) {
            residentApi
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
                    "SELECT id, resident_id FROM " + spec.table() + " WHERE resident_id IN (" + in + ")" + and()
                            + " ORDER BY id",
                    rs -> {
                        candidates.add(new Candidate(rs.getLong(1), expired.get(rs.getLong(2))));
                    });
        }
        return RetentionProvider.page(candidates.stream(), after, batch);
    }

    /** Bản ghi của hồ sơ người cao tuổi {@code target} (000 T112, FR-051a). */
    @Override
    public Set<DataRef> referencing(DataRef target) {
        if (!SharedRowRetention.RESIDENT.equals(target.dataType())) {
            return Set.of();
        }
        return jdbc
                .queryForList(
                        "SELECT id FROM " + spec.table() + " WHERE resident_id = ?" + and(), Long.class, target.id())
                .stream()
                .map(id -> new DataRef(dataType(), id))
                .collect(Collectors.toSet());
    }

    @Override
    public Set<DataRef> referencesTo(long id) {
        return spec.children().stream()
                .flatMap(child -> jdbc.queryForList(child.sql(), Long.class, id).stream()
                        .map(childId -> new DataRef(child.dataType(), childId)))
                .collect(Collectors.toCollection(java.util.LinkedHashSet::new));
    }

    @Override
    public void delete(Collection<Long> ids) {
        String in = ids.stream().map(String::valueOf).collect(Collectors.joining(",", "(", ")"));
        database.jdbc().update("DELETE FROM " + spec.table() + " WHERE id IN " + in);
    }

    private String and() {
        return spec.where().isEmpty() ? "" : " AND " + spec.where();
    }
}
