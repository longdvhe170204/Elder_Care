package com.eldercare.backend.roombedallocation;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.eldercare.backend.administration.account.api.spi.InvalidPermissionDataException;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.resident.api.dto.ResidenceType;
import com.eldercare.backend.resident.api.spi.ContractApi;
import com.eldercare.backend.roombedallocation.api.LocationStructureApi;
import com.eldercare.backend.roombedallocation.api.dto.LocationRef;
import com.eldercare.backend.roombedallocation.internal.allocation.persistence.AllocationStore;
import com.eldercare.backend.roombedallocation.internal.allocation.service.AllocationQueries;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class Q403_DayCareFloorTest {
    @Mock
    AllocationStore store;

    @Mock
    BusinessTime time;

    @Mock
    LocationStructureApi locations;

    @Mock
    ContractApi contracts;

    @InjectMocks
    AllocationQueries queries;

    @Test
    void Q_403_resolvesOnlyAnUnambiguousDayCareFloor() {
        var at = Instant.parse("2026-11-08T03:00:00Z");
        var day = LocalDate.of(2026, 11, 8);
        when(contracts.currentResidenceType(1L, day)).thenReturn(Optional.of(ResidenceType.DAY_CARE));
        when(locations.active(any())).thenReturn(true);
        when(locations.dayCareRestAreaLocation()).thenReturn(Optional.of(new LocationRef("FLOOR", 2L)));
        assertThat(queries.dayCareFloorAt(1L, at)).contains(2L);
        when(locations.dayCareRestAreaLocation()).thenReturn(Optional.of(new LocationRef("AREA", 4L)));
        when(locations.floorsInArea(4L)).thenReturn(Set.of(2L));
        assertThat(queries.dayCareFloorAt(1L, at)).contains(2L);
        when(locations.floorsInArea(4L)).thenReturn(Set.of(2L, 3L));
        assertThatThrownBy(() -> queries.dayCareFloorAt(1L, at)).isInstanceOf(InvalidPermissionDataException.class);
        when(locations.floorsInArea(4L)).thenReturn(Set.of());
        assertThat(queries.dayCareFloorAt(1L, at)).isEmpty();
        when(contracts.currentResidenceType(1L, day)).thenReturn(Optional.of(ResidenceType.LONG_TERM_RESIDENTIAL));
        assertThat(queries.dayCareFloorAt(1L, at)).isEmpty();
    }
}
