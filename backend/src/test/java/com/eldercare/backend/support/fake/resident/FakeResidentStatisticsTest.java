package com.eldercare.backend.support.fake.resident;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.administration.account.api.dto.ScopeFilter;
import com.eldercare.backend.resident.api.dto.CareLevel;
import com.eldercare.backend.resident.api.dto.OverdueReassessmentView;
import com.eldercare.backend.resident.api.dto.ResidentChangeView;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import com.eldercare.backend.resident.api.dto.ResidentStatusAtDateView;
import com.eldercare.backend.resident.api.dto.StatusCommand;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Bản giả {@code ResidentStatisticsApi} cho 016 (team-plan mục 8): dữ liệu dựng sẵn, lọc theo {@link ScopeFilter}. */
class FakeResidentStatisticsTest {

    private static final LocalDate DAY = LocalDate.of(2026, 10, 1);
    private static final Instant T = Instant.parse("2026-10-01T03:00:00Z");

    private static ScopeFilter floor(long id) {
        return new ScopeFilter(false, Set.of(id), Set.of(), Set.of());
    }

    @Test
    void statusAtDate_returnsTheDaysRows_filteredByFloorRoomOrResident() {
        FakeResidentStatistics stats = new FakeResidentStatistics()
                .resident(1, 10L, 100L)
                .resident(2, 20L, 200L)
                .statusOn(DAY, 1, ResidentStatus.IN_RESIDENCE, CareLevel.BASIC)
                .statusOn(DAY, 2, ResidentStatus.HOSPITALIZED, null)
                .statusOn(DAY.plusDays(1), 1, ResidentStatus.TEMPORARILY_ABSENT, CareLevel.BASIC);

        assertThat(stats.statusAtDate(ScopeFilter.FACILITY, DAY))
                .containsExactlyInAnyOrder(
                        new ResidentStatusAtDateView(1, ResidentStatus.IN_RESIDENCE, Optional.of(CareLevel.BASIC)),
                        new ResidentStatusAtDateView(2, ResidentStatus.HOSPITALIZED, Optional.empty()));
        assertThat(stats.statusAtDate(floor(10), DAY))
                .extracting(ResidentStatusAtDateView::residentId)
                .containsExactly(1L);
        assertThat(stats.statusAtDate(new ScopeFilter(false, Set.of(), Set.of(200L), Set.of()), DAY))
                .extracting(ResidentStatusAtDateView::residentId)
                .containsExactly(2L);
        assertThat(stats.statusAtDate(new ScopeFilter(false, Set.of(), Set.of(), Set.of(2L)), DAY))
                .extracting(ResidentStatusAtDateView::residentId)
                .containsExactly(2L);
    }

    @Test
    void changes_returnsOnlyChangesInTheHalfOpenRange_andInScope() {
        ResidentChangeView inside = new ResidentChangeView(
                1,
                T,
                Optional.of(StatusCommand.GRANT_TEMPORARY_ABSENCE),
                Optional.of(ResidentStatus.IN_RESIDENCE),
                Optional.of(ResidentStatus.TEMPORARILY_ABSENT),
                Optional.empty());
        ResidentChangeView atEnd = new ResidentChangeView(
                1,
                T.plusSeconds(60),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.of(CareLevel.SPECIAL));
        FakeResidentStatistics stats = new FakeResidentStatistics()
                .resident(1, 10L, null)
                .change(inside)
                .change(atEnd);

        assertThat(stats.changes(ScopeFilter.FACILITY, T, T.plusSeconds(60))).containsExactly(inside);
        assertThat(stats.changes(floor(99), T, T.plusSeconds(120))).isEmpty();
    }

    @Test
    void overdueReassessments_areFilteredByScope() {
        FakeResidentStatistics stats = new FakeResidentStatistics()
                .resident(1, 10L, null)
                .resident(2, 20L, null)
                .overdue(1, T)
                .overdue(2, T);

        assertThat(stats.overdueReassessments(floor(20))).containsExactly(new OverdueReassessmentView(2, T));
    }
}
