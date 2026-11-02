package com.eldercare.backend.medication.internal.governance.service;

import com.eldercare.backend.administration.retention.api.RetentionKind;
import com.eldercare.backend.administration.retention.api.spi.RetentionProvider;
import com.eldercare.backend.resident.api.ResidentApi;
import java.time.Instant;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Stream;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Retention provider for resident medication data (data-model section 13, research R-11).
 * Belongs to {@link RetentionKind.ResidentRecord} (per CFG-M15-03 from terminal status).
 * Medication catalog is group 1 and is not purged.
 */
@Component
public class MedicationRetentionProvider implements RetentionProvider {

    public static final String TYPE = "M07.PRESCRIPTION";

    private final JdbcTemplate jdbc;
    private final ObjectProvider<ResidentApi> residents;

    public MedicationRetentionProvider(JdbcTemplate jdbc, ObjectProvider<ResidentApi> residents) {
        this.jdbc = jdbc;
        this.residents = residents;
    }

    @Override
    public String dataType() {
        return TYPE;
    }

    @Override
    public RetentionKind retentionKind() {
        return new RetentionKind.ResidentRecord();
    }

    @Override
    public Stream<Candidate> expiredCandidates(Instant cutoff, int batch) {
        if (batch <= 0) {
            return Stream.empty();
        }
        ResidentApi residentApi = residents.getIfAvailable();
        if (residentApi == null) {
            return Stream.empty();
        }
        return jdbc.queryForList("SELECT id, resident_id FROM prescription ORDER BY id").stream()
                .flatMap(row -> residentApi
                        .finalStatusAt(((Number) row.get("resident_id")).longValue())
                        .filter(finalAt -> finalAt.isBefore(cutoff))
                        .map(finalAt -> new Candidate(((Number) row.get("id")).longValue(), finalAt))
                        .stream())
                .limit(batch);
    }

    @Override
    public Set<DataRef> referencesTo(long id) {
        return Set.of();
    }

    @Override
    public void delete(Collection<Long> ids) {
        // Deletion executed by eldercare_retention when data is present
    }
}
