package com.eldercare.backend.medication.internal.service;

import com.eldercare.backend.administration.retention.api.RetentionKind;
import com.eldercare.backend.administration.retention.api.spi.RetentionProvider;
import java.time.Instant;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;

/**
 * Retention provider for resident medication data (data-model section 13, research R-11).
 * Belongs to {@link RetentionKind.ResidentRecord} (per CFG-M15-03 from terminal status).
 * Medication catalog is group 1 and is not purged.
 */
@Component
public class MedicationRetentionProvider implements RetentionProvider {

    public static final String TYPE = "M07.PRESCRIPTION";

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
        return Stream.empty();
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
