package com.eldercare.backend.administration.account.internal.scope.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.eldercare.backend.administration.account.api.spi.*;
import com.eldercare.backend.administration.parameter.api.*;
import com.eldercare.backend.support.fake.administration.FakeAssignments;
import java.time.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import tools.jackson.databind.json.JsonMapper;

/** FR-025/BR-M09-15: mốc kết thúc sớm chặn cả phần biên CFG-M15-07 của ca. */
class EarlyAssignmentEndTest {
    @Test
    @SuppressWarnings("unchecked")
    void FR_032_scopeIncludesTheExactStartOfTheConfiguredMargin() {
        Instant start = Instant.parse("2026-11-01T00:00:00Z");
        var rows = new FakeAssignments()
                .assign(
                        1,
                        new AssignmentView(
                                AssignmentKind.SHIFT_MEMBER, 10L, null, null, 20L, start, start.plusSeconds(3600)));
        ObjectProvider<AssignmentApi> source = mock(ObjectProvider.class);
        when(source.getIfAvailable()).thenReturn(rows);
        var parameters = mock(ParameterApi.class);
        when(parameters.read(ParameterCode.CFG_M15_07))
                .thenReturn(new ParameterValue(
                        ParameterCode.CFG_M15_07,
                        ParameterKind.DURATION,
                        JsonMapper.builder().build().readTree("{\"value\":2,\"unit\":\"HOURS\"}")));
        var active = new ActiveAssignments(source, mock(ObjectProvider.class), parameters);
        assertThat(active.at(1, start.minusSeconds(7200))).hasSize(1);
        assertThat(active.at(1, start.minusSeconds(7200).minusNanos(1))).isEmpty();
    }

    @Test
    void BR_M09_15_earlyEndCutsOffScopeAtTheExactInstantAndHistoricalRange() {
        Instant start = Instant.parse("2026-11-01T00:00:00Z"),
                end = start.plusSeconds(8 * 3600),
                cut = start.plusSeconds(3600);
        var rows = new FakeAssignments()
                .assign(1, new AssignmentView(AssignmentKind.SHIFT_MEMBER, 10L, null, null, 20L, start, end, cut));
        @SuppressWarnings("unchecked")
        ObjectProvider<AssignmentApi> source = mock(ObjectProvider.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<BedAllocationApi> beds = mock(ObjectProvider.class);
        when(source.getIfAvailable()).thenReturn(rows);
        var parameters = mock(ParameterApi.class);
        when(parameters.read(ParameterCode.CFG_M15_07))
                .thenReturn(new ParameterValue(
                        ParameterCode.CFG_M15_07,
                        ParameterKind.DURATION,
                        JsonMapper.builder().build().readTree("{\"value\":2,\"unit\":\"HOURS\"}")));
        var active = new ActiveAssignments(source, beds, parameters);
        assertThat(active.at(1, cut.minusNanos(1))).hasSize(1);
        assertThat(active.at(1, cut)).isEmpty();
        assertThat(active.at(1, end.plusSeconds(60))).isEmpty();
        assertThat(active.during(1, cut, cut.plusSeconds(60))).isEmpty();
        assertThat(active.during(1, cut.minusSeconds(60), cut)).hasSize(1);
        assertThat(rows.activeAt(1, cut)).isEmpty();
    }
}
