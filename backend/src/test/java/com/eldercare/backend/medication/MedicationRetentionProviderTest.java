package com.eldercare.backend.medication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.eldercare.backend.administration.retention.api.RetentionDatabase;
import com.eldercare.backend.administration.retention.api.spi.RetentionProvider;
import com.eldercare.backend.medication.internal.governance.persistence.MedicationRetentionProvider;
import com.eldercare.backend.medication.internal.governance.persistence.RetentionSpec;
import com.eldercare.backend.resident.api.ResidentApi;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;

class MedicationRetentionProviderTest {

    private static final RetentionSpec PRESCRIPTION =
            new RetentionSpec("M07.PRESCRIPTION", "prescription", "", List.of());

    @SuppressWarnings("unchecked")
    private static ObjectProvider<ResidentApi> providerOf(ResidentApi residents) {
        ObjectProvider<ResidentApi> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(residents);
        return provider;
    }

    @Test
    void F_06_onlyRowsOfResidentsFinalBeforeCutoffAreCandidates() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        ResidentApi residents = mock(ResidentApi.class);
        when(residents.byStatus(org.mockito.ArgumentMatchers.anySet())).thenReturn(List.of(1L, 2L, 3L));
        when(residents.finalStatusAt(1L)).thenReturn(Optional.of(Instant.parse("2020-01-01T00:00:00Z")));
        when(residents.finalStatusAt(2L)).thenReturn(Optional.of(Instant.parse("2030-01-01T00:00:00Z")));
        when(residents.finalStatusAt(3L)).thenReturn(Optional.empty());
        org.mockito.Mockito.doAnswer(call -> {
                    RowCallbackHandler handler = call.getArgument(1);
                    java.sql.ResultSet rs = mock(java.sql.ResultSet.class);
                    when(rs.getLong(1)).thenReturn(101L);
                    when(rs.getLong(2)).thenReturn(1L);
                    handler.processRow(rs);
                    return null;
                })
                .when(jdbc)
                .query(anyString(), org.mockito.ArgumentMatchers.<RowCallbackHandler>any());

        MedicationRetentionProvider retention = new MedicationRetentionProvider(
                PRESCRIPTION, jdbc, mock(RetentionDatabase.class), providerOf(residents));

        assertThat(retention.expiredCandidates(Instant.parse("2025-01-01T00:00:00Z"), null, 10))
                .containsExactly(new RetentionProvider.Candidate(101L, Instant.parse("2020-01-01T00:00:00Z")));
    }

    @Test
    void F_06_missingResidentSourceKeepsData() {
        MedicationRetentionProvider retention = new MedicationRetentionProvider(
                PRESCRIPTION, mock(JdbcTemplate.class), mock(RetentionDatabase.class), providerOf(null));

        assertThat(retention.expiredCandidates(Instant.parse("2025-01-01T00:00:00Z"), 10))
                .isEmpty();
    }
}
