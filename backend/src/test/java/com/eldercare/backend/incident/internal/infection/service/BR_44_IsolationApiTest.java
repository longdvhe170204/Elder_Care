package com.eldercare.backend.incident.internal.infection.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.eldercare.backend.administration.shared.api.dto.SimpleRecord;
import com.eldercare.backend.incident.api.dto.IsolationZoneView;
import com.eldercare.backend.incident.internal.infection.domain.ContainmentContent;
import com.eldercare.backend.incident.internal.infection.domain.ContainmentPeriod;
import com.eldercare.backend.incident.internal.infection.domain.ContainmentScope;
import com.eldercare.backend.incident.internal.infection.persistence.ContainmentStore;
import com.eldercare.backend.roombedallocation.api.LocationStructureApi;
import com.eldercare.backend.roombedallocation.api.ResidentLocationApi;
import com.eldercare.backend.roombedallocation.api.dto.LocationView;
import com.eldercare.backend.roombedallocation.api.dto.RoomView;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * BR-M05-11, FR-064 (012 FR-030 (c)): vùng khoanh có hiệu lực tại thời điểm hỏi theo tầng, khu vực, phòng và người cao
 * tuổi (vị trí của người lúc đó); vùng đã gỡ không còn tính từ lúc gỡ.
 */
class BR_44_IsolationApiTest {
    private static final Instant T0 = Instant.parse("2026-11-08T01:00:00Z");

    private final ContainmentStore store = mock(ContainmentStore.class);
    private final ResidentLocationApi residents = mock(ResidentLocationApi.class);
    private final LocationStructureApi structure = mock(LocationStructureApi.class);
    private final DefaultIsolationApi api = new DefaultIsolationApi(store, residents, structure);

    private static SimpleRecord<ContainmentContent> zone(long id, ContainmentScope scope, Instant released) {
        var content = new ContainmentContent(new ContainmentPeriod(scope, T0, released), "Cúm", Set.of(1L), null, null);
        return new SimpleRecord<>(
                id, null, null, released == null ? "ACTIVE" : "RELEASED", content, null, null, null, null, T0, 1, 0);
    }

    @Test
    void BR_44_zonesByFloorAreaRoomAndResidentAtTime() {
        when(store.list())
                .thenReturn(List.of(
                        zone(1, new ContainmentScope(Set.of(2L), Set.of()), null),
                        zone(2, new ContainmentScope(Set.of(), Set.of(9L)), T0.plusSeconds(3600))));
        when(structure.floorsInArea(9L)).thenReturn(Set.of(3L));
        when(structure.room(20L)).thenReturn(Optional.of(new RoomView(20, 2, 1, "P201", "P201", true)));
        var inArea = new LocationView(9L, "Khu A", 3L, "Tầng 3", 30L, "P301", 1L, "G1");
        when(residents.at(7L, T0.plusSeconds(60))).thenReturn(Optional.of(inArea));
        when(residents.at(7L, T0.plusSeconds(7200))).thenReturn(Optional.of(inArea));
        when(residents.at(8L, T0.plusSeconds(60))).thenReturn(Optional.empty());

        assertThat(api.activeByFloorArea(2L, T0.plusSeconds(60)))
                .extracting(IsolationZoneView::id)
                .containsExactly(1L);
        assertThat(api.activeByFloorArea(3L, T0.plusSeconds(60)))
                .extracting(IsolationZoneView::id)
                .containsExactly(2L);
        assertThat(api.activeByRoom(20L, T0.plusSeconds(60)))
                .extracting(IsolationZoneView::id)
                .containsExactly(1L);
        assertThat(api.activeByResident(7L, T0.plusSeconds(60)))
                .extracting(IsolationZoneView::id)
                .containsExactly(2L);
        assertThat(api.activeByResident(7L, T0.plusSeconds(7200)))
                .as("vùng đã gỡ")
                .isEmpty();
        assertThat(api.activeByResident(8L, T0.plusSeconds(60)))
                .as("không có vị trí")
                .isEmpty();
        assertThat(api.activeZones()).extracting(IsolationZoneView::id).containsExactly(1L);
        assertThat(api.activeByFloorArea(2L, T0.minusSeconds(1)))
                .as("trước khi đặt")
                .isEmpty();
    }
}
