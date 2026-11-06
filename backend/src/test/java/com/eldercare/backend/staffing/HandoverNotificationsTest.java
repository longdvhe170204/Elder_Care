package com.eldercare.backend.staffing;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.common.notification.*;
import com.eldercare.backend.staffing.internal.handover.service.HandoverNotifications;
import com.eldercare.backend.staffing.internal.shift.domain.WorkShift;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/** FR-057: đính chính báo người bàn giao, người xác nhận và Người phụ trách ca hiện tại. */
class HandoverNotificationsTest {
    private final Instant at = Instant.parse("2026-10-27T01:00:00Z");
    private final WorkShift previous =
            new WorkShift(20, 1, 3, at.minusSeconds(28800), at, "FLOOR", 5L, true, "CLOSED", 10L, false, 0);
    private final HandoverNotifications notifications = new HandoverNotifications();

    @Test
    void FR_057_correctionTargetsTheCurrentShiftLeadAlongsideTheOriginalParticipants() {
        var request = notifications.corrected(30, 10, 11L, previous, at).getFirst();

        assertThat(request.recipientGroups())
                .containsExactly(
                        new RecipientGroup.Staff(10, "HANDOVER_CORRECTED"),
                        new RecipientGroup.Staff(11, "HANDOVER_CORRECTED"),
                        new RecipientGroup.ShiftLead(5, new Substitution.Default()));
        assertThat(request.level()).contains(NotificationLevel.LOW);
        assertThat(request.sourceRecord()).isEqualTo(new SourceRecord("M09.HANDOVER", "30"));
    }

    @Test
    void FR_057_oneStaffWhoGivesAndConfirmsReceivesOnlyOneParticipantNotification() {
        var request = notifications.corrected(30, 10, 10L, previous, at).getFirst();

        assertThat(request.recipientGroups())
                .containsExactly(
                        new RecipientGroup.Staff(10, "HANDOVER_CORRECTED"),
                        new RecipientGroup.ShiftLead(5, new Substitution.Default()));
    }
}
