package com.cambae.backend.m09;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cambae.backend.common.Role;
import com.cambae.backend.m09.assignment.internal.domain.PrimaryAssigneeResolver;
import com.cambae.backend.m09.assignment.internal.domain.PrimaryAssigneeResolver.TargetRef;
import com.cambae.backend.m09.assignment.internal.domain.StaffAssignment;
import com.cambae.backend.m15.account.api.spi.InvalidPermissionDataException;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;

/** T017, FR-028: ưu tiên đối tượng trước giới hạn công việc; không tự chọn khi dữ liệu cùng ưu tiên mâu thuẫn. */
class PrimaryAssigneeResolverTest {
    private final Instant at = Instant.parse("2026-11-01T03:00:00Z");
    private final Set<TargetRef> position =
            Set.of(new TargetRef("RESIDENT", 31), new TargetRef("ROOM", 21), new TargetRef("FLOOR", 11));

    private StaffAssignment assignment(
            long id, String type, long target, Long task, String kind, String status, Instant end) {
        return new StaffAssignment(
                id,
                1,
                id + 100,
                type,
                target,
                task,
                Role.CAREGIVER,
                kind,
                false,
                at.minusSeconds(3600),
                end,
                status,
                null,
                "MANUAL",
                0);
    }

    private StaffAssignment assignment(long id, String type, long target, Long task) {
        return assignment(id, type, target, task, "PRIMARY", "ACTIVE", at.plusSeconds(3600));
    }

    @Test
    void FR_028_residentOverridesRoomAndFloorEvenIfTheirTaskTypeIsMoreSpecific() {
        var floor = assignment(1, "FLOOR", 11, 7L);
        var room = assignment(2, "ROOM", 21, 7L);
        var direct = assignment(3, "RESIDENT", 31, null);
        assertThat(PrimaryAssigneeResolver.resolve(List.of(floor, room, direct), position, Role.CAREGIVER, 7L, at))
                .contains(direct);
    }

    @Test
    void FR_028_sameLevelTaskTypeOverridesUnrestrictedAndOtherTypesAreIgnored() {
        var unrestricted = assignment(1, "ROOM", 21, null);
        var specific = assignment(2, "ROOM", 21, 7L);
        var other = assignment(3, "ROOM", 21, 8L);
        assertThat(PrimaryAssigneeResolver.resolve(
                        List.of(unrestricted, other, specific), position, Role.CAREGIVER, 7L, at))
                .contains(specific);
        assertThat(PrimaryAssigneeResolver.resolve(
                        List.of(unrestricted, other, specific), position, Role.CAREGIVER, 9L, at))
                .contains(unrestricted);
    }

    @Test
    void FR_028_supportAndOtherResidentsOrRolesCannotBecomePrimary() {
        var support = assignment(1, "RESIDENT", 31, null, "SUPPORT", "ACTIVE", at.plusSeconds(60));
        var other = assignment(2, "RESIDENT", 32, null);
        assertThat(PrimaryAssigneeResolver.resolve(List.of(support, other), position, Role.CAREGIVER, 7L, at))
                .isEmpty();
        assertThat(PrimaryAssigneeResolver.resolve(
                        List.of(assignment(3, "FLOOR", 11, null)), position, Role.NURSING_STAFF, null, at))
                .isEmpty();
    }

    @Test
    void BR_M09_15_draftsCancelledAndEndedDoNotCreateAssignments() {
        var rows = List.of("DRAFT", "CANCELLED", "ENDED").stream()
                .map(status -> assignment(
                        1,
                        "FLOOR",
                        11,
                        null,
                        "PRIMARY",
                        status,
                        "ENDED".equals(status) ? at.minusSeconds(1) : at.plusSeconds(60)))
                .toList();
        assertThat(PrimaryAssigneeResolver.resolve(rows, position, Role.CAREGIVER, null, at))
                .isEmpty();
    }

    @Test
    void FR_037a_intervalIncludesStartAndExcludesEnd() {
        var row = assignment(1, "FLOOR", 11, null);
        assertThat(PrimaryAssigneeResolver.resolve(List.of(row), position, Role.CAREGIVER, null, row.startsAt()))
                .contains(row);
        assertThat(PrimaryAssigneeResolver.resolve(List.of(row), position, Role.CAREGIVER, null, row.endsAt()))
                .isEmpty();
    }

    @Test
    void FR_028_equalPrecedenceFromConflictingSourceFailsClosed() {
        assertThatThrownBy(() -> PrimaryAssigneeResolver.resolve(
                        List.of(assignment(1, "ROOM", 21, null), assignment(2, "ROOM", 21, null)),
                        position,
                        Role.CAREGIVER,
                        null,
                        at))
                .isInstanceOf(InvalidPermissionDataException.class);
    }

    @Test
    void FR_037a_endedAssignmentStillIdentifiesTheHistoricalPrimaryBeforeItsActualEnd() {
        var original = assignment(1, "FLOOR", 11, null);
        var ended = new StaffAssignment(
                original.id(),
                original.shiftId(),
                original.staffId(),
                original.targetType(),
                original.targetId(),
                original.taskTypeId(),
                original.role(),
                original.assignmentKind(),
                false,
                original.startsAt(),
                original.endsAt(),
                "ENDED",
                at.minusSeconds(60),
                "MANUAL",
                1);
        assertThat(PrimaryAssigneeResolver.resolve(
                        List.of(ended), position, Role.CAREGIVER, null, at.minusSeconds(120)))
                .contains(ended);
        assertThat(PrimaryAssigneeResolver.resolve(List.of(ended), position, Role.CAREGIVER, null, at))
                .isEmpty();
    }

    @Test
    void FR_028_primarySelectionIsStableAcrossTwoHundredInputOrders() {
        var winner = assignment(4, "RESIDENT", 31, 7L);
        var rows = new ArrayList<>(List.of(
                assignment(1, "FLOOR", 11, null),
                assignment(2, "ROOM", 21, null),
                assignment(3, "RESIDENT", 31, null),
                winner));
        var random = new Random(8);
        for (int i = 0; i < 200; i++) {
            Collections.shuffle(rows, random);
            assertThat(PrimaryAssigneeResolver.resolve(rows, position, Role.CAREGIVER, 7L, at))
                    .contains(winner);
        }
    }
}
