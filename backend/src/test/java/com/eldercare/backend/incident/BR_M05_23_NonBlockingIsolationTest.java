package com.eldercare.backend.incident;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.eldercare.backend.incident.internal.infection.service.ContainmentStatusParticipant;
import com.eldercare.backend.incident.internal.infection.service.IncidentAllocationStatus;
import com.eldercare.backend.resident.api.dto.CommandContext;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import com.eldercare.backend.resident.api.dto.StatusCommand;
import com.eldercare.backend.roombedallocation.api.ResidentLocationApi;
import com.eldercare.backend.roombedallocation.api.dto.LocationView;
import com.eldercare.backend.roombedallocation.api.spi.CordonedArea;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * BR-M05-23, FR-065 (CHK008): Cho tạm vắng, Chuyển viện với người trong vùng đang khoanh vùng không bị chặn, chỉ có
 * cảnh báo cho người thực hiện; người ngoài vùng không có cảnh báo.
 */
class BR_M05_23_NonBlockingIsolationTest {
    private static final Instant AT = Instant.parse("2026-11-06T03:00:00Z");
    private final IncidentAllocationStatus zones = mock(IncidentAllocationStatus.class);
    private final ResidentLocationApi locations = mock(ResidentLocationApi.class);
    private final ContainmentStatusParticipant participant = new ContainmentStatusParticipant(zones, locations);

    @Test
    void BR_M05_23_temporaryAbsenceAndTransferOnlyWarnInsideZone() {
        when(zones.cordonedAt(AT)).thenReturn(new CordonedArea(Set.of(2L), Set.of(9L)));
        when(locations.current(1L)).thenReturn(Optional.of(location(1L, 2L)));
        when(locations.current(3L)).thenReturn(Optional.of(location(8L, 4L)));
        when(locations.current(5L)).thenReturn(Optional.of(location(9L, 6L)));

        assertThat(participant.interestedCommands())
                .containsExactlyInAnyOrder(StatusCommand.GRANT_TEMPORARY_ABSENCE, StatusCommand.TRANSFER_OUT);
        for (var command : participant.interestedCommands()) {
            assertThat(participant.checkConditions(context(1L, command))).isEmpty();
            assertThat(participant.warnings(context(1L, command)))
                    .extracting(w -> w.code())
                    .containsExactly(ContainmentStatusParticipant.IN_ISOLATION_ZONE);
            assertThat(participant.warnings(context(3L, command))).isEmpty();
            assertThat(participant.warnings(context(5L, command))).hasSize(1);
        }
    }

    @Test
    void BR_M05_23_residentWithoutBedHasNoWarning() {
        when(zones.cordonedAt(AT)).thenReturn(new CordonedArea(Set.of(2L), Set.of()));
        when(locations.current(7L)).thenReturn(Optional.empty());

        assertThat(participant.warnings(context(7L, StatusCommand.TRANSFER_OUT)))
                .isEmpty();
    }

    private static LocationView location(long areaId, long floorId) {
        return new LocationView(areaId, "Khu", floorId, "Tầng", 10L, "P", 11L, "G");
    }

    private static CommandContext context(long residentId, StatusCommand command) {
        return new CommandContext(residentId, ResidentStatus.IN_RESIDENCE, command, null, AT, null);
    }
}
