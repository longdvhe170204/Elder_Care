package com.eldercare.backend.incident;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.PermissionApi;
import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.administration.account.api.dto.PermissionKey;
import com.eldercare.backend.common.error.PermissionDeniedException;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.incident.api.spi.AlertStaffPort;
import com.eldercare.backend.incident.internal.incident.domain.IncidentFloor;
import com.eldercare.backend.incident.internal.incident.service.IncidentFloorQueries;
import com.eldercare.backend.roombedallocation.api.LocationStructureApi;
import com.eldercare.backend.roombedallocation.api.dto.FloorView;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** BR-M05-17, BR-M05-29: tầng đang trực đứng đầu, rồi các tầng đang có ca; không cần quyền cơ cấu phòng, giường. */
class FR_040_IncidentFloorQueriesTest {
    private static final Instant NOW = Instant.parse("2026-11-23T08:00:00Z");

    private final PermissionApi permissions = mock(PermissionApi.class);
    private final BusinessTime time = mock(BusinessTime.class);
    private final AlertStaffPort staff = mock(AlertStaffPort.class);
    private final LocationStructureApi structure = mock(LocationStructureApi.class);
    private final CurrentUser user = mock(CurrentUser.class);
    private final IncidentFloorQueries queries = new IncidentFloorQueries(permissions, time, staff, structure);

    private void floor(long id, String name, boolean active) {
        when(structure.floor(id)).thenReturn(Optional.of(new FloorView(id, 1, "T", name, "Nhà A", active)));
    }

    @Test
    void BR_M05_17_ownFloorFirstThenFloorsOnShift() {
        when(time.now()).thenReturn(NOW);
        when(user.staffId()).thenReturn(Optional.of(31L));
        when(permissions.permissions(user))
                .thenReturn(Set.of(new PermissionKey(Function.INCIDENT_EMERGENCY, Operation.CREATE)));
        when(staff.floorDuringShift(31L, NOW)).thenReturn(Optional.of(2L));
        when(staff.floorsWithShift(NOW)).thenReturn(List.of(1L, 2L, 3L));
        floor(1, "Tầng 1", true);
        floor(2, "Tầng 2", true);
        floor(3, "Tầng 3", false);

        assertThat(queries.floors(user))
                .containsExactly(
                        new IncidentFloor(2, "Tầng 2", "Nhà A", true), new IncidentFloor(1, "Tầng 1", "Nhà A", false));
    }

    @Test
    void FR_080_withoutRecordOrAssistPermissionIsDenied() {
        when(permissions.permissions(any())).thenReturn(Set.of());
        assertThatThrownBy(() -> queries.floors(user)).isInstanceOf(PermissionDeniedException.class);
    }
}
