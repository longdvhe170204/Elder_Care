package com.eldercare.backend.incident;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.eldercare.backend.incident.internal.alert.domain.IncidentLevel;
import com.eldercare.backend.incident.internal.alert.service.AlertEscalationService;
import com.eldercare.backend.incident.internal.alert.service.AlertResponsibility;
import com.eldercare.backend.incident.internal.alert.service.AlertService;
import com.eldercare.backend.roombedallocation.api.LocationHistoryApi;
import com.eldercare.backend.roombedallocation.api.ResidentLocationApi;
import com.eldercare.backend.roombedallocation.api.dto.LocationView;
import com.eldercare.backend.roombedallocation.api.dto.ResidentLocationRow;
import com.eldercare.backend.support.TestUsers;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Người cao tuổi vắng mặt (BR-M05-19, FR-031, CHK006): tầng của cảnh báo là tầng của giường đang giữ, không còn giường
 * thì tầng của phân bổ gần nhất; hạn tiếp nhận và leo thang không dừng vì vắng mặt.
 */
class BR_M05_19_AbsentResidentIT extends AlertRequestTestBase {
    private final ResidentLocationApi locations = mock(ResidentLocationApi.class);
    private final LocationHistoryApi history = mock(LocationHistoryApi.class);

    private void useLocations() {
        service = new AlertService(
                alerts,
                published::add,
                parameters,
                time,
                monitoringLock,
                null,
                null,
                recordEvents,
                new AlertResponsibility(null, null, null, locations, history));
    }

    @Test
    void BR_M05_19_absentResidentKeepsFloorOfHeldBed() {
        long heldFloor = TestUsers.nextId();
        when(locations.current(resident)).thenReturn(Optional.of(location(heldFloor)));
        useLocations();

        var alert = request("VUOT_NGUONG:5", IncidentLevel.MEDIUM, T0, 1);

        assertThat(alert.floorAreaId()).isEqualTo(heldFloor);
    }

    @Test
    void BR_M05_19_noBedUsesFloorOfLatestAllocation() {
        long oldFloor = TestUsers.nextId();
        long latestFloor = TestUsers.nextId();
        when(locations.current(resident)).thenReturn(Optional.empty());
        when(history.byResident(eq(resident), any(), any()))
                .thenReturn(List.of(
                        row(oldFloor, T0.minus(Duration.ofDays(60)), T0.minus(Duration.ofDays(30))),
                        row(latestFloor, T0.minus(Duration.ofDays(30)), T0.minus(Duration.ofDays(2)))));
        useLocations();

        var alert = request("VUOT_NGUONG:5", IncidentLevel.MEDIUM, T0, 1);

        assertThat(alert.floorAreaId()).isEqualTo(latestFloor);
    }

    @Test
    void CHK006_deadlineAndEscalationContinueWhileAbsent() {
        when(locations.current(resident)).thenReturn(Optional.empty());
        when(history.byResident(eq(resident), any(), any())).thenReturn(List.of());
        useLocations();
        var alert = request("VUOT_NGUONG:5", IncidentLevel.MEDIUM, T0, 1);
        assertThat(alert.acknowledgementDeadline()).isEqualTo(T0.plus(Duration.ofMinutes(15)));

        var escalation = new AlertEscalationService(alerts, published::add, parameters, transaction, null, null);
        escalation.escalate(alert.id(), T0.plus(Duration.ofMinutes(16)));

        assertThat(alerts.findById(alert.id()).orElseThrow().escalationLevel()).isEqualTo(2);
    }

    private static LocationView location(long floorId) {
        return new LocationView(null, null, floorId, "Tầng", 1L, "P1", 1L, "G1");
    }

    private ResidentLocationRow row(long floorId, java.time.Instant from, java.time.Instant to) {
        return new ResidentLocationRow(resident, "Người A", location(floorId), from, to, false, false, List.of());
    }
}
