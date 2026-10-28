package com.cambae.backend.m01;

import static org.assertj.core.api.Assertions.assertThat;

import com.cambae.backend.m01.internal.profile.service.ResidentProfileService.ProfileView;
import com.cambae.backend.m15.account.api.spi.AssignmentKind;
import com.cambae.backend.m15.account.api.spi.AssignmentView;
import com.cambae.backend.m15.account.api.spi.BedAllocationView;
import com.cambae.backend.support.TestUsers;
import com.cambae.backend.support.TestUsers.TestUser;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * 001 T019 (research H-05, NFR-02; 002 FR-034): tra cứu hồ sơ của vai trò có phạm vi theo phân công lọc bằng bộ lọc
 * phạm vi của 002, trong đó tầng, phòng được phân công đổi thành người cao tuổi đang có giường ở đó (phân bổ giường của
 * 003). Điều dưỡng chỉ tra được người ở tầng hoặc phòng mình; Bác sĩ (toàn viện) tra được cả hai.
 */
class ResidentSearchScopeIT extends HealthTestBase {

    private long inFloor(long floor) {
        long id = newResident();
        bedAllocations.allocate(
                id, new BedAllocationView(TestUsers.nextId(), TestUsers.nextId(), TestUsers.nextId(), floor));
        return id;
    }

    private void assign(TestUser who, Long floor, Long room) {
        assignments.assign(
                who.staffId(),
                new AssignmentView(
                        AssignmentKind.SHIFT_MEMBER,
                        floor,
                        room,
                        null,
                        null,
                        time.now().minus(Duration.ofDays(1)),
                        null));
    }

    private List<Long> found(TestUser who, Set<Long> among) {
        user.set(who.user());
        return profiles.search("Nguyễn Văn Sức Khỏe", Set.of(), null, 100).items().stream()
                .map(ProfileView::id)
                .filter(among::contains)
                .toList();
    }

    @Test
    void T019_nurseAssignedToAFloor_findsOnlyResidentsWithABedOnThatFloor() {
        long floor = TestUsers.nextId();
        long mine = inFloor(floor);
        long other = inFloor(TestUsers.nextId());
        assign(nurse, floor, null);

        assertThat(found(nurse, Set.of(mine, other))).containsExactly(mine);
        assertThat(found(doctor, Set.of(mine, other))).containsExactlyInAnyOrder(mine, other);
    }

    @Test
    void T019_nurseAssignedToARoom_findsOnlyResidentsWithABedInThatRoom() {
        long room = TestUsers.nextId();
        long mine = newResident();
        bedAllocations.allocate(mine, new BedAllocationView(TestUsers.nextId(), TestUsers.nextId(), room, 1L));
        long other = inFloor(TestUsers.nextId());
        assign(nurse, null, room);

        assertThat(found(nurse, Set.of(mine, other))).containsExactly(mine);
    }

    @Test
    void T019_residentWithoutABed_isNotInAFloorScope() {
        long floor = TestUsers.nextId();
        long noBed = newResident();
        assign(nurse, floor, null);

        assertThat(found(nurse, Set.of(noBed))).isEmpty();
    }
}
