package com.eldercare.backend.roombedallocation.internal.allocation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.eldercare.backend.administration.account.api.spi.ResidencyEndApi;
import com.eldercare.backend.administration.parameter.api.ParameterApi;
import com.eldercare.backend.administration.retention.api.RetentionDatabase;
import com.eldercare.backend.administration.retention.api.RetentionKind;
import com.eldercare.backend.administration.retention.api.spi.RetentionProvider.Candidate;
import com.eldercare.backend.administration.retention.api.spi.RetentionProvider.DataRef;
import com.eldercare.backend.administration.shared.api.SharedRowRetention;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.roombedallocation.internal.allocation.domain.AllocationResident;
import com.eldercare.backend.roombedallocation.internal.allocation.persistence.M03GovernanceStore;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

/**
 * 000 T112 (module-interfaces 000 mục 1.7, FR-051, FR-051a) cho 003: phân bổ giường của người cao tuổi trả qua
 * {@code referencing} để hồ sơ không bị xóa trước; ứng viên lấy theo trang sau mốc {@code (at, id)}.
 */
class T112_M03RetentionTest {
    private static final Instant CUTOFF = Instant.parse("2026-11-01T00:00:00Z");
    private static final Instant ENDED = Instant.parse("2026-09-01T00:00:00Z");

    private final M03GovernanceStore store = mock(M03GovernanceStore.class);
    private final ResidencyEndApi ends = mock(ResidencyEndApi.class);

    @SuppressWarnings("unchecked")
    private M03Retention allocations() {
        ObjectProvider<ResidencyEndApi> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(ends);
        return new M03Retention(
                "M03.BED_ALLOCATION",
                "bed_allocation",
                new RetentionKind.ResidentRecord(),
                store,
                mock(RetentionDatabase.class),
                provider,
                mock(ParameterApi.class),
                mock(BusinessTime.class));
    }

    @Test
    void T112_allocationsOfTheResidentAreReturnedByReferencing() {
        when(store.allocationsOf(41L)).thenReturn(List.of(301L, 302L));

        assertThat(allocations().referencing(new DataRef(SharedRowRetention.RESIDENT, 41L)))
                .containsExactlyInAnyOrder(
                        new DataRef("M03.BED_ALLOCATION", 301L), new DataRef("M03.BED_ALLOCATION", 302L));
        assertThat(allocations().referencing(new DataRef("M03.BED", 41L))).isEmpty();
    }

    @Test
    void T112_allocationCandidatesArePagedByEndTimeThenId() {
        when(store.allocations())
                .thenAnswer(call -> Stream.of(
                        new AllocationResident(12, 41),
                        new AllocationResident(11, 41),
                        new AllocationResident(13, 42)));
        when(ends.finalStatusAt(41L)).thenReturn(Optional.of(ENDED));
        when(ends.finalStatusAt(42L)).thenReturn(Optional.empty());

        var first = allocations().expiredCandidates(CUTOFF, null, 1).toList();
        var second = allocations().expiredCandidates(CUTOFF, first.getLast(), 1).toList();

        assertThat(first).containsExactly(new Candidate(11, ENDED));
        assertThat(second).containsExactly(new Candidate(12, ENDED));
    }
}
