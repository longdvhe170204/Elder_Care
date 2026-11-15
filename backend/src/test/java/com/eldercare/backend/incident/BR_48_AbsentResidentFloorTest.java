package com.eldercare.backend.incident;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.eldercare.backend.administration.parameter.api.ParameterApi;
import com.eldercare.backend.administration.parameter.api.dto.ParameterCode;
import com.eldercare.backend.administration.parameter.api.dto.ParameterValue;
import com.eldercare.backend.incident.api.dto.AlertShiftView;
import com.eldercare.backend.incident.api.spi.AlertStaffPort;
import com.eldercare.backend.incident.internal.alert.domain.IncidentLevel;
import com.eldercare.backend.incident.internal.alert.service.AcknowledgementDeadlines;
import com.eldercare.backend.incident.internal.alert.service.AlertResponsibility;
import com.eldercare.backend.roombedallocation.api.LocationHistoryApi;
import com.eldercare.backend.roombedallocation.api.ResidentLocationApi;
import com.eldercare.backend.roombedallocation.api.dto.LocationView;
import com.eldercare.backend.roombedallocation.api.dto.ResidentLocationRow;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** BR-M05-19: tầng của người vắng mặt, hạn tiếp nhận cảnh báo Nhẹ tạo sau mốc lập bàn giao. */
class BR_48_AbsentResidentFloorTest {
    private static final Instant NOW = Instant.parse("2030-03-04T03:00:00Z");
    private static final long RESIDENT = 7L;

    private static LocationView floor(long id) {
        return new LocationView(null, null, id, "Tầng " + id, null, null, 0L, null);
    }

    private static ResidentLocationRow row(long floor, Instant from) {
        return new ResidentLocationRow(RESIDENT, "[Họ tên]", floor(floor), from, null, false, false, List.of());
    }

    @Test
    void BR_48_floorIsHeldBedFloorElseLatestAllocationFloor() {
        var locations = mock(ResidentLocationApi.class);
        var history = mock(LocationHistoryApi.class);
        var responsibility = new AlertResponsibility(null, null, null, locations, history);

        when(locations.current(RESIDENT)).thenReturn(Optional.of(floor(11)));
        assertThat(responsibility.floorOf(RESIDENT, NOW)).contains(11L);

        when(locations.current(RESIDENT)).thenReturn(Optional.empty());
        when(history.byResident(RESIDENT, Instant.EPOCH, NOW))
                .thenReturn(List.of(row(21, NOW.minusSeconds(7200)), row(22, NOW.minusSeconds(3600))));
        assertThat(responsibility.floorOf(RESIDENT, NOW)).contains(22L);
    }

    @Test
    void BR_48_lowAlertAfterHandoverCutoffIsDueAtEndOfNextShift() {
        var parameters = mock(ParameterApi.class);
        var lead = mock(ParameterValue.class);
        when(lead.asDuration()).thenReturn(Duration.ofMinutes(30));
        when(parameters.read(ParameterCode.CFG_M09_04)).thenReturn(lead);
        var staff = mock(AlertStaffPort.class);
        var next = new AlertShiftView(2L, 10L, NOW.plusSeconds(1200), NOW.plusSeconds(30000));
        when(staff.nextShift(eq(10L), any())).thenReturn(Optional.of(next));

        var current = new AlertShiftView(1L, 10L, NOW.minusSeconds(3600), NOW.plusSeconds(7200));
        when(staff.shiftAt(10L, NOW)).thenReturn(Optional.of(current));
        assertThat(AcknowledgementDeadlines.of(IncidentLevel.LOW, NOW, 10L, parameters, Optional.of(staff)))
                .isEqualTo(current.endsAt());

        var ending = new AlertShiftView(1L, 10L, NOW.minusSeconds(3600), NOW.plusSeconds(1200));
        when(staff.shiftAt(10L, NOW)).thenReturn(Optional.of(ending));
        assertThat(AcknowledgementDeadlines.of(IncidentLevel.LOW, NOW, 10L, parameters, Optional.of(staff)))
                .isEqualTo(next.endsAt());
    }
}
