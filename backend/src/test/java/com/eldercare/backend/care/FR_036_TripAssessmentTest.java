package com.eldercare.backend.care;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.eldercare.backend.administration.account.api.spi.BedAllocationView;
import com.eldercare.backend.care.internal.trip.domain.Trip;
import com.eldercare.backend.care.internal.trip.persistence.ActivityTripStore;
import com.eldercare.backend.care.internal.trip.service.TripPreparationService;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.support.fake.administration.FakeBedAllocations;
import java.time.*;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class FR_036_TripAssessmentTest {
    @Test
    void Q_403_dayCareAssessmentUsesRestAreaFloorAndAnAllocatedBedTakesPrecedence() {
        var store = mock(ActivityTripStore.class);
        var time = new BusinessTime(Clock.fixed(Instant.parse("2026-11-08T03:00:00Z"), BusinessTime.ZONE));
        var beds = new FakeBedAllocations().dayCare(5L, 7L);
        when(store.find(1L)).thenReturn(Optional.of(new Trip(1L, 2L, 3L, null, "SCHEDULED", 0)));
        var service = new TripPreparationService(store, null, null, null, null, beds, null, null, time, null);
        assertThat(service.assessmentTarget(1L, 5L).floorAreaId()).isEqualTo(7L);
        beds.allocate(5L, new BedAllocationView(11L, 12L, 13L, 8L));
        assertThat(service.assessmentTarget(1L, 5L).floorAreaId()).isEqualTo(8L);
    }
}
