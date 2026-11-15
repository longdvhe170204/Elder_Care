package com.eldercare.backend.notification;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.common.notification.NotificationLevel;
import com.eldercare.backend.notification.api.dto.RecipientType;
import com.eldercare.backend.notification.internal.delivery.domain.ChannelSelection;
import com.eldercare.backend.notification.internal.delivery.domain.ChannelSelection.Channel;
import com.eldercare.backend.notification.internal.delivery.domain.ChannelSelection.Planned;
import com.eldercare.backend.notification.internal.delivery.domain.ChannelSelection.Recipient;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * BR-M13-01, FR-016, FR-016a, FR-020 (009 T023, tên logic {@code ChonKenhTest}): kênh theo mức và loại người nhận; nhân
 * viên ngoài ca không nhận tin nhắn Trung bình; kênh tắt không dùng; kênh khác chỉ ở mức được gắn; nội dung kênh ngoài
 * không hợp lệ, không có địa chỉ ghi kết quả ngay.
 */
class ChannelSelectionTest {

    private static final Channel SMS_ON = new Channel("SMS", true, Set.of());
    private static final Channel SMS_OFF = new Channel("SMS", false, Set.of());
    private static final Channel ZALO_URGENT = new Channel("OTHER_ZALO", true, EnumSet.of(NotificationLevel.URGENT));

    private static Recipient staff(boolean onShift) {
        return new Recipient(RecipientType.STAFF, true, Optional.of("0901000001"), onShift);
    }

    private static List<Planned> plan(NotificationLevel level, Recipient r, List<Channel> channels) {
        return ChannelSelection.plan(level, r, false, channels);
    }

    @Test
    void BR_75_staff_lowInAppOnly_mediumAddsSms_urgentEveryChannel() {
        List<Channel> channels = List.of(SMS_ON, ZALO_URGENT);

        assertThat(plan(NotificationLevel.LOW, staff(true), channels))
                .containsExactly(new Planned("IN_APP", Optional.empty()));
        assertThat(plan(NotificationLevel.MEDIUM, staff(true), channels))
                .containsExactly(new Planned("IN_APP", Optional.empty()), new Planned("SMS", Optional.empty()));
        assertThat(plan(NotificationLevel.URGENT, staff(true), channels))
                .containsExactly(
                        new Planned("IN_APP", Optional.empty()),
                        new Planned("SMS", Optional.empty()),
                        new Planned("OTHER_ZALO", Optional.empty()));
    }

    @Test
    void BR_75_family_portalAndSms_resident_inAppOnly() {
        Recipient family = new Recipient(RecipientType.FAMILY_MEMBER, true, Optional.of("0901000002"), true);
        Recipient resident = new Recipient(RecipientType.RESIDENT, true, Optional.empty(), true);

        assertThat(plan(NotificationLevel.MEDIUM, family, List.of(SMS_ON)))
                .containsExactly(new Planned("FAMILY_PORTAL", Optional.empty()), new Planned("SMS", Optional.empty()));
        assertThat(plan(NotificationLevel.LOW, resident, List.of(SMS_ON, ZALO_URGENT)))
                .containsExactly(new Planned("IN_APP", Optional.empty()));
    }

    @Test
    void FR_016_disabledChannelNotUsed_noAccountNoInApp() {
        Recipient noAccount = new Recipient(RecipientType.FAMILY_MEMBER, false, Optional.of("0901000003"), true);

        assertThat(plan(NotificationLevel.MEDIUM, staff(true), List.of(SMS_OFF)))
                .containsExactly(new Planned("IN_APP", Optional.empty()));
        assertThat(plan(NotificationLevel.MEDIUM, noAccount, List.of(SMS_ON)))
                .containsExactly(new Planned("SMS", Optional.empty()));
    }
}
