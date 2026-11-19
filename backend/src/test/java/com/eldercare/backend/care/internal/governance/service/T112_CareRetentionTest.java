package com.eldercare.backend.care.internal.governance.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.eldercare.backend.administration.retention.api.RetentionDatabase;
import com.eldercare.backend.administration.retention.api.spi.RetentionProvider.Candidate;
import com.eldercare.backend.administration.retention.api.spi.RetentionProvider.DataRef;
import com.eldercare.backend.administration.shared.api.SharedRowRetention;
import com.eldercare.backend.care.internal.governance.domain.CareRetentionRow;
import com.eldercare.backend.care.internal.governance.domain.CareRetentionType;
import com.eldercare.backend.care.internal.governance.persistence.CareGovernanceStore;
import com.eldercare.backend.resident.api.ResidentApi;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

/**
 * 000 T112 (module-interfaces 000 mục 1.7, FR-051, FR-051a) cho 005: bản ghi gắn hồ sơ trả qua {@code referencing} để
 * hồ sơ không bị xóa trước; ứng viên lấy theo trang sau mốc {@code (at, id)}.
 */
class T112_CareRetentionTest {
    private static final Instant CUTOFF = Instant.parse("2026-11-01T00:00:00Z");
    private static final long RESIDENT = 77L;

    private final CareGovernanceStore store = mock(CareGovernanceStore.class);
    private final ResidentApi residents = mock(ResidentApi.class);

    @SuppressWarnings("unchecked")
    private CareRetention retention(CareRetentionType type) {
        ObjectProvider<ResidentApi> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(residents);
        return new CareRetention(type, store, mock(RetentionDatabase.class), provider);
    }

    @Test
    void T112_profileRecordsOfTheResidentAreReturnedByReferencing() {
        when(store.rowsOfResident(CareRetentionType.TASK, RESIDENT))
                .thenReturn(List.of(new CareRetentionRow(5, RESIDENT, null), new CareRetentionRow(9, RESIDENT, null)));

        assertThat(retention(CareRetentionType.TASK).referencing(new DataRef(SharedRowRetention.RESIDENT, RESIDENT)))
                .containsExactlyInAnyOrder(new DataRef("M04.TASK", 5), new DataRef("M04.TASK", 9));
        assertThat(retention(CareRetentionType.TASK).referencing(new DataRef("M04.CARE_PLAN", RESIDENT)))
                .isEmpty();
        assertThat(retention(CareRetentionType.LOCATION_TASK)
                        .referencing(new DataRef(SharedRowRetention.RESIDENT, RESIDENT)))
                .isEmpty();
    }

    @Test
    void T112_candidatesArePagedAfterTheLastOfThePreviousPage() {
        when(store.rows(CareRetentionType.ACTIVITY_SESSION))
                .thenReturn(List.of(
                        new CareRetentionRow(3, null, Instant.parse("2026-10-03T00:00:00Z")),
                        new CareRetentionRow(1, null, Instant.parse("2026-10-01T00:00:00Z")),
                        new CareRetentionRow(2, null, Instant.parse("2026-10-02T00:00:00Z")),
                        new CareRetentionRow(4, null, Instant.parse("2026-11-05T00:00:00Z"))));
        var provider = retention(CareRetentionType.ACTIVITY_SESSION);

        var first = provider.expiredCandidates(CUTOFF, null, 2).toList();
        var second = provider.expiredCandidates(CUTOFF, first.getLast(), 2).toList();

        assertThat(first).extracting(Candidate::id).containsExactly(1L, 2L);
        assertThat(second).extracting(Candidate::id).containsExactly(3L);
    }

    @Test
    void T112_profileCandidatesUseTheFinalStatusTime() {
        when(store.rows(CareRetentionType.TASK))
                .thenReturn(List.of(new CareRetentionRow(8, RESIDENT, null), new CareRetentionRow(6, RESIDENT, null)));
        when(residents.finalStatusAt(RESIDENT)).thenReturn(Optional.of(Instant.parse("2026-09-01T00:00:00Z")));

        var page = retention(CareRetentionType.TASK)
                .expiredCandidates(CUTOFF, new Candidate(6, Instant.parse("2026-09-01T00:00:00Z")), 10)
                .toList();

        assertThat(page).containsExactly(new Candidate(8, Instant.parse("2026-09-01T00:00:00Z")));
    }
}
