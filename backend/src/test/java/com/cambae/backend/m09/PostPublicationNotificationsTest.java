package com.cambae.backend.m09;

import static org.assertj.core.api.Assertions.*;

import com.cambae.backend.common.notification.*;
import com.cambae.backend.m09.shift.internal.domain.WorkShift;
import com.cambae.backend.m09.shift.internal.service.PostPublicationNotifications;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class PostPublicationNotificationsTest {
    final Instant at = Instant.parse("2026-10-25T00:00:00Z");
    final WorkShift shift =
            new WorkShift(20, 1, 3, at, at.plusSeconds(28800), "FLOOR", 5L, true, "IN_PROGRESS", 10L, false, 0);
    final PostPublicationNotifications notices = new PostPublicationNotifications(null);

    @Test
    void FR_058_shiftChangesAndHandoverReassignmentAreLow_absenceAndMissingLeadAreMedium() {
        for (String event : List.of("STAFF_ADDED", "LEAD_CHANGED", "CANCELLED", "SHIFT_SWAP_015"))
            assertThat(notices.request(event, shift, List.of(10L), at, false)
                            .getFirst()
                            .level())
                    .contains(NotificationLevel.LOW);
        assertThat(notices.handover(shift, 30, 11, at).getFirst().level()).contains(NotificationLevel.LOW);
        for (String event : List.of("STAFF_ABSENT", "LEAD_MISSING"))
            assertThat(notices.request(event, shift, List.of(10L), at, false)
                            .getFirst()
                            .level())
                    .contains(NotificationLevel.MEDIUM);
    }

    @Test
    void FR_059_externalContentIdentifiesShiftAndScope_andHandoverUsesItsOwnSource() {
        var request =
                notices.request("STAFF_ABSENT", shift, List.of(10L), at, false).getFirst();
        assertThat(request.floorAreaId()).contains(5L);
        assertThat(request.shortContent())
                .hasValueSatisfying(text -> assertThat(text).contains("Ca #20", "tầng #5", "Mở ứng dụng"));
        var handover = notices.handover(shift, 30, 11, at).getFirst();
        assertThat(handover.sourceRecord()).isEqualTo(new SourceRecord("M09.HANDOVER", "30"));
        assertThat(handover.sourceGroup()).isEqualTo(SourceGroup.SCHEDULE_CHANGE);
    }
}
