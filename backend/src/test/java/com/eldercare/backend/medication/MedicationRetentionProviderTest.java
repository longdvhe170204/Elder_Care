package com.eldercare.backend.medication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.eldercare.backend.administration.retention.api.spi.RetentionProvider;
import com.eldercare.backend.medication.internal.governance.service.MedicationRetentionProvider;
import com.eldercare.backend.resident.api.ResidentApi;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;

class MedicationRetentionProviderTest {

    @Test
    void F_06_onlyPrescriptionsOfResidentsFinalBeforeCutoffAreCandidates() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        ResidentApi residents = mock(ResidentApi.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<ResidentApi> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(residents);
        when(jdbc.queryForList("SELECT id, resident_id FROM prescription ORDER BY id"))
                .thenReturn(List.of(
                        Map.of("id", 101L, "resident_id", 1L),
                        Map.of("id", 102L, "resident_id", 2L),
                        Map.of("id", 103L, "resident_id", 3L)));
        when(residents.finalStatusAt(1L)).thenReturn(Optional.of(Instant.parse("2020-01-01T00:00:00Z")));
        when(residents.finalStatusAt(2L)).thenReturn(Optional.of(Instant.parse("2030-01-01T00:00:00Z")));
        when(residents.finalStatusAt(3L)).thenReturn(Optional.empty());

        MedicationRetentionProvider retention = new MedicationRetentionProvider(jdbc, provider);

        assertThat(retention.expiredCandidates(Instant.parse("2025-01-01T00:00:00Z"), 10))
                .containsExactly(new RetentionProvider.Candidate(101L, Instant.parse("2020-01-01T00:00:00Z")));
    }

    @Test
    void F_06_missingResidentSourceKeepsData() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<ResidentApi> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(null);

        MedicationRetentionProvider retention = new MedicationRetentionProvider(jdbc, provider);

        assertThat(retention.expiredCandidates(Instant.parse("2025-01-01T00:00:00Z"), 10))
                .isEmpty();
    }
}
