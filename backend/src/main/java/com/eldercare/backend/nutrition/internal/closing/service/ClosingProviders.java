package com.eldercare.backend.nutrition.internal.closing.service;

import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.administration.account.api.spi.ScopeAssignmentProvider;
import com.eldercare.backend.administration.retention.api.RetentionDatabase;
import com.eldercare.backend.administration.retention.api.RetentionKind;
import com.eldercare.backend.administration.retention.api.spi.RetentionProvider;
import com.eldercare.backend.administration.shared.api.SharedRowRetention;
import com.eldercare.backend.administration.shared.api.SimpleRecordApi;
import com.eldercare.backend.nutrition.internal.closing.domain.ClosingRecords.FamilyMeal;
import com.eldercare.backend.nutrition.internal.closing.domain.ClosingRecords.MealSlip;
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
import org.springframework.transaction.annotation.Transactional;

/** Provider của chốt suất (T007): lưu giữ suất ăn người thân theo hồ sơ; phạm vi phiếu bữa ăn theo tầng/khu. */
final class ClosingProviders {

    private ClosingProviders() {}

    /**
     * Lưu giữ suất ăn người thân ({@code LuuTruProvider} {@code THEO_HO_SO}; T007, research M-15) như bản gán: theo hồ sơ
     * người cao tuổi được gắn (CFG-M15-03), tính từ lúc người đó sang trạng thái cuối. Mẫu {@code M10SimpleRecordRetention}.
     */
    @Component
    static class FamilyMealRetention implements RetentionProvider {

        private static final int CHUNK = 500;
        private static final Set<ResidentStatus> FINAL_STATUSES =
                Set.of(ResidentStatus.RESIDENCY_ENDED, ResidentStatus.DECEASED, ResidentStatus.ADMISSION_CANCELLED);

        private final JdbcTemplate jdbc;
        private final RetentionDatabase database;
        private final ResidentApi residents;

        FamilyMealRetention(JdbcTemplate jdbc, RetentionDatabase database, ResidentApi residents) {
            this.jdbc = jdbc;
            this.database = database;
            this.residents = residents;
        }

        @Override
        public String dataType() {
            return FamilyMeal.KIND.code();
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
                        dataType());
            }
            return RetentionProvider.page(candidates.stream(), after, batch);
        }

        /** Suất ăn người thân gắn hồ sơ người cao tuổi {@code target} (000 T112, FR-051a). */
        @Override
        public Set<DataRef> referencing(DataRef target) {
            if (!SharedRowRetention.RESIDENT.equals(target.dataType())) {
                return Set.of();
            }
            return jdbc
                    .queryForList(
                            "SELECT id FROM simple_record WHERE kind = ? AND resident_id = ?",
                            Long.class,
                            dataType(),
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
            database.jdbc().update("DELETE FROM simple_record WHERE kind = ? AND id IN " + in, dataType());
        }
    }

    /** Gắn phạm vi cho phiếu bữa ăn ({@code GanPhamViProvider} {@code M08.MEAL_SLIP}; T007): tầng/khu của phiếu. */
    @Component
    static class MealSlipScopeAssignment implements ScopeAssignmentProvider {

        private final SimpleRecordApi records;

        MealSlipScopeAssignment(SimpleRecordApi records) {
            this.records = records;
        }

        @Override
        public String entityType() {
            return MealSlip.KIND.code();
        }

        @Override
        @Transactional(readOnly = true)
        public Target assignScope(long id) {
            MealSlip slip = records.require(MealSlip.KIND, id).content();
            return new Target(entityType(), Long.toString(id), null, slip.floorAreaId());
        }
    }
}
