package com.eldercare.backend.staffing;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.common.Role;
import com.eldercare.backend.staffing.internal.assignment.domain.PrimaryAssigneeResolver;
import com.eldercare.backend.staffing.internal.assignment.domain.PrimaryAssigneeResolver.TargetRef;
import com.eldercare.backend.staffing.internal.assignment.domain.StaffAssignment;
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
}
