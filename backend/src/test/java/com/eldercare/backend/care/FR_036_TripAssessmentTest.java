package com.eldercare.backend.care;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.eldercare.backend.administration.account.api.spi.BedAllocationView;
import com.eldercare.backend.care.internal.trip.domain.Trip;
import com.eldercare.backend.care.internal.trip.persistence.ActivityTripStore;
import com.eldercare.backend.care.internal.trip.service.TripPreparationService;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.error.PermissionDeniedException;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.support.fake.administration.FakeBedAllocations;
import java.time.*;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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

    /**
     * T015 Q20: Nhân viên chăm sóc làm trưởng đoàn và Điều dưỡng có quyền T trên "Hoạt động, ngoài viện" nhưng không đánh
     * giá khả năng; Trưởng tầng qua được bước vai trò (PermissionMatrixIT không phủ vì chặn ở lệnh).
     */
    @Test
    void FR_036_onlyTheFloorManagerAssessesNotTheTripLeaderOrANurse() {
        var store = mock(ActivityTripStore.class);
        when(store.lock(1L)).thenReturn(new Trip(1L, 2L, 3L, null, "IN_PROGRESS", 0));
        var user = mock(com.eldercare.backend.administration.account.api.CurrentUser.class);
        var service = new TripPreparationService(store, null, null, user, null, null, null, null, null, null);
        com.eldercare.backend.support.TableRows.each(
                List.of(Role.CAREGIVER, Role.NURSING_STAFF, Role.FLOOR_MANAGER), role -> {
                    when(user.roles()).thenReturn(Set.of(role));
                    var thrown = catchThrowable(() -> service.assess(null, 1L, 5L, "PASS", null));
                    if (role == Role.FLOOR_MANAGER)
                        assertThat(thrown)
                                .isInstanceOfSatisfying(
                                        BusinessException.class,
                                        e -> assertThat(e.errorCode()).isEqualTo("STATE_CHANGED"));
                    else assertThat(thrown).isInstanceOf(PermissionDeniedException.class);
                });
    }
}
