package com.eldercare.backend.staffing;

import static org.assertj.core.api.Assertions.*;

import com.eldercare.backend.roombedallocation.api.dto.LocationRef;
import com.eldercare.backend.roombedallocation.api.events.*;
import com.eldercare.backend.support.fake.roombedallocation.FakeLocations;
import java.time.*;
import org.junit.jupiter.api.Test;

class StaffingSourceContractsTest {

    @Test
    void FR_035_allocationEventsPreserveResidentOldAndNewFloorsAndEffectiveInstant() {
        var at = Instant.parse("2026-10-25T00:00:00Z");
        assertThat(new AllocationStarted(1, 2, 3, at).residentId()).isEqualTo(2);
        assertThat(new AllocationClosed(1, 2, 3, at, "Kết thúc").at()).isEqualTo(at);
        var moved = new BedChanged(2, 10, 11, 20L, 21L, at);
        assertThat(moved.previousFloorId()).isEqualTo(20);
        assertThat(moved.newFloorId()).isEqualTo(21);
        var locations = new FakeLocations();
        assertThat(locations.dayCareRestAreaLocation()).isEmpty();
        locations.daycareRestLocation(new LocationRef("FLOOR", 20));
        assertThat(locations.dayCareRestAreaLocation()).contains(new LocationRef("FLOOR", 20));
        locations.floor(20, null).room(22, 20).stay(2, 22, at.minusSeconds(3600), null);
        locations.closeStays(2, at);
        assertThat(locations.at(2, at.minusSeconds(1))).isPresent();
        assertThat(locations.at(2, at)).isEmpty();
    }
}
