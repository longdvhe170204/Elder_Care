package com.eldercare.backend.staffing;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.eldercare.backend.admission.api.ContractReadApi;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.resident.api.*;
import com.eldercare.backend.resident.api.dto.*;
import com.eldercare.backend.staffing.internal.assignment.service.*;
import com.eldercare.backend.staffing.internal.shift.domain.WorkShift;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

class AssignmentReadSourcesTest {
    private static <T> ObjectProvider<T> provider(T value) {
        @SuppressWarnings("unchecked")
        ObjectProvider<T> p = mock(ObjectProvider.class);
        when(p.getIfAvailable()).thenReturn(value);
        return p;
    }

    @Test
    void FR_029_missingAttendanceBeanFailsClosed() {
        var sources = mock(AssignmentSources.class);
        var people = mock(ResidentApi.class);
        var shift = mock(WorkShift.class);
        when(shift.startsAt()).thenReturn(Instant.parse("2026-11-07T15:00:00Z"));
        when(shift.scopeType()).thenReturn("FLOOR");
        when(shift.scopeId()).thenReturn(2L);
        when(people.byStatus(anySet())).thenReturn(List.of(1L));
        var reads = new AssignmentReadSources(
                sources, provider(people), provider(mock(ResidentStatusApi.class)), provider(null));
        assertThatThrownBy(() -> reads.expected(shift))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        e -> assertThat(e.errorCode()).isEqualTo("ASSIGNMENT_SOURCE_UNAVAILABLE"));
    }

    @Test
    void FR_029_historicalAbsenceIsExcludedAtTheExactStart() {
        var sources = mock(AssignmentSources.class);
        var people = mock(ResidentApi.class);
        var statuses = mock(ResidentStatusApi.class);
        var attendance = mock(ContractReadApi.class);
        var at = Instant.parse("2026-11-07T15:00:00Z");
        var shift = mock(WorkShift.class);
        when(shift.startsAt()).thenReturn(at);
        when(shift.scopeType()).thenReturn("FLOOR");
        when(shift.scopeId()).thenReturn(2L);
        when(sources.residents("FLOOR", 2, at)).thenReturn(Set.of(1L));
        when(people.byStatus(anySet())).thenReturn(List.of(1L));
        when(statuses.statusPeriods(eq(1L), anySet(), eq(at), eq(at.plusNanos(1000))))
                .thenReturn(List.of(new StatusPeriod(ResidentStatus.HOSPITALIZED, at, Optional.empty())));
        var reads = new AssignmentReadSources(sources, provider(people), provider(statuses), provider(attendance));
        assertThat(reads.expected(shift)).isEmpty();
        verify(sources, never()).position(anyLong(), any());
    }
}
