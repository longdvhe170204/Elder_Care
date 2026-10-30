package com.eldercare.backend.staffing;

import static org.assertj.core.api.Assertions.*;

import com.eldercare.backend.care.api.dto.TaskRequirements;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import com.eldercare.backend.roombedallocation.api.dto.LocationRef;
import com.eldercare.backend.support.fake.care.FakeCareRequirements;
import com.eldercare.backend.support.fake.resident.FakeResidents;
import com.eldercare.backend.support.fake.roombedallocation.FakeLocations;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;

/** T018: bản giả chung lưu nguồn riêng, không mặc định dữ liệu thiếu thành đủ điều kiện. */
class AssignmentSourcesTest {
    @Test
    void FR_026_locationSourceResolvesThePositionAtTimeIncludingRoomMoves() {
        var source = new FakeLocations().floor(10, 1L).room(20, 10).room(21, 10);
        Instant at = Instant.parse("2026-11-01T00:00:00Z");
        source.stay(30, 20, at, at.plusSeconds(3600)).stay(30, 21, at.plusSeconds(3600), at.plusSeconds(7200));
        assertThat(source.at(30, at).orElseThrow().roomId()).isEqualTo(20);
        assertThat(source.at(30, at.plusSeconds(3600)).orElseThrow().roomId()).isEqualTo(21);
        assertThat(source.byResident(30, at, at.plusSeconds(7200))).hasSize(2);
        assertThat(source.byLocation(new LocationRef("FLOOR", 10), at)).hasSize(1);
        assertThat(source.active(new LocationRef("ROOM", 999))).isFalse();
    }

    @Test
    void FR_030_missingTaskRequirementsDoNotBecomeAnEmptyRequirement() {
        var source = new FakeCareRequirements();
        assertThatThrownBy(() -> source.requirements(7)).isInstanceOf(NoSuchElementException.class);
        source.require(7, new TaskRequirements(Role.NURSING_STAFF, Set.of("NURSING_LICENSE")));
        source.plan(30, Set.of(7L));
        assertThat(source.requirements(7).role()).isEqualTo(Role.NURSING_STAFF);
        assertThat(source.currentTaskTypes(30)).containsExactly(7L);
        assertThat(new FakeCareRequirements().currentTaskTypes(30)).isEmpty();
    }

    @Test
    void FR_026_residentFinalStatusIsExplicitAndDoesNotLeakAcrossFakes() {
        var source = new FakeResidents()
                .resident(30, "[Họ tên NCT 30]", null, ResidentStatus.IN_RESIDENCE)
                .resident(31, "[Họ tên NCT 31]", null, ResidentStatus.DECEASED);
        assertThat(source.isFinalStatus(30)).isFalse();
        assertThat(source.isFinalStatus(31)).isTrue();
        assertThatThrownBy(() -> new FakeResidents().isFinalStatus(30)).isInstanceOf(IllegalArgumentException.class);
    }
}
