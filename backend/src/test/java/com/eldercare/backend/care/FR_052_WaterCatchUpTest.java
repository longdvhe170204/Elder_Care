package com.eldercare.backend.care;

import static org.mockito.Mockito.*;

import com.eldercare.backend.care.internal.rules.service.*;
import com.eldercare.backend.resident.api.ResidentApi;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class FR_052_WaterCatchUpTest {
    @Test
    void FR_052_sameDayCatchUpIncludesResidentWhoLeftAfterTheMilestone() {
        var residents = mock(ResidentApi.class);
        var rules = mock(CareResultRules.class);
        when(residents.byStatus(anySet())).thenAnswer(invocation -> {
            Set<ResidentStatus> statuses = invocation.getArgument(0);
            return statuses.contains(ResidentStatus.RESIDENCY_ENDED) ? List.of(123L) : List.of();
        });
        var now = Instant.parse("2026-11-02T10:00:00Z");
        new CareWaterCheckStep(residents, rules).run(now);
        verify(rules).water(123L, LocalDate.of(2026, 11, 2), now);
    }
}
