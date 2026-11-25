package com.eldercare.backend.staffing;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.notification.NotificationRequest;
import com.eldercare.backend.common.notification.RecipientGroup;
import com.eldercare.backend.staffing.internal.handover.service.HandoverNotifications;
import com.eldercare.backend.staffing.internal.shift.domain.WorkShift;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * team-plan mục 8 (2026-11-18, D): ca toàn viện không có tầng (scopeId rỗng). Thông báo bàn giao không gửi được cho
 * Trưởng tầng, Phụ trách ca theo tầng nên gửi Quản lý viện thay, không ném NullPointerException.
 */
class HandoverNotificationsFacilityShiftTest {
    private static final Instant AT = Instant.parse("2035-05-01T07:00:00Z");
    private static final WorkShift FACILITY = new WorkShift(
            98, 1, 1, AT, AT.plus(Duration.ofHours(8)), "FACILITY", null, true, "IN_PROGRESS", null, true, 0);
    private final HandoverNotifications notifications = new HandoverNotifications();

    private static List<RecipientGroup> recipients(List<NotificationRequest> requests) {
        return requests.getFirst().recipientGroups();
    }

    @Test
    void facilityShift_handoverNoticesGoToManagerInsteadOfFloorRoles() {
        var manager = new RecipientGroup.FacilityRole(Role.NURSING_HOME_MANAGER);
        assertThat(recipients(notifications.prepared(1, FACILITY, null, AT))).contains(manager);
        assertThat(recipients(notifications.feedback(1, 11, FACILITY, AT))).contains(manager);
        assertThat(recipients(
                        notifications.reminder("HANDOVER_REMINDER", 1, "M09.HANDOVER", FACILITY, AT, "Nhắc", true)))
                .contains(manager);
        assertThat(recipients(notifications.corrected(1, 11, null, FACILITY, AT)))
                .contains(manager);
    }
}
