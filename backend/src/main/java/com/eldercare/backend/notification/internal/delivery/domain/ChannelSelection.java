package com.eldercare.backend.notification.internal.delivery.domain;

import com.eldercare.backend.common.notification.NotificationLevel;
import com.eldercare.backend.notification.api.dto.RecipientType;
import com.eldercare.backend.notification.internal.domain.DeliveryOutcome;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Chọn kênh ({@code ChonKenh}; BR-M13-01, FR-016, FR-016a, FR-018, FR-020, research B-05): kênh trong ứng dụng khi có
 * tài khoản; tin nhắn cho mức Trung bình, Khẩn cấp; kênh khác ở mức Quản lý viện gắn; người cao tuổi chỉ trong ứng dụng.
 * Lần gửi biết trước kết quả (ngoài ca, nội dung không hợp lệ, không có địa chỉ) ghi kết quả ngay, không gửi.
 */
public final class ChannelSelection {

    public static final String SMS = "SMS";
    public static final String OTHER_PREFIX = "OTHER_";

    /** Kênh đã cấu hình; {@code levels} chỉ dùng cho kênh khác. */
    public record Channel(String code, boolean enabled, Set<NotificationLevel> levels) {}

    /**
     * @param address số điện thoại cho tin nhắn
     * @param onShift nhân viên đang trong ca (FR-016a); người thân, người cao tuổi bỏ qua
     */
    public record Recipient(RecipientType type, boolean hasAccount, Optional<String> address, boolean onShift) {}

    /** Một lần gửi dự kiến; {@code immediate} có thì ghi kết quả đó ngay. */
    public record Planned(String channel, Optional<DeliveryOutcome> immediate) {}

    private ChannelSelection() {}

    public static List<Planned> plan(
            NotificationLevel level, Recipient r, boolean invalidOutOfAppContent, List<Channel> channels) {
        List<Planned> planned = new ArrayList<>();
        if (r.hasAccount()) {
            planned.add(new Planned(inApp(r.type()), Optional.empty()));
        }
        if (r.type() == RecipientType.RESIDENT) {
            return planned;
        }
        for (Channel c : channels) {
            if (!c.enabled()) {
                continue;
            }
            if (c.code().equals(SMS) && level != NotificationLevel.LOW) {
                planned.add(new Planned(SMS, sms(level, r, invalidOutOfAppContent)));
            } else if (c.code().startsWith(OTHER_PREFIX) && c.levels().contains(level)) {
                planned.add(new Planned(
                        c.code(),
                        invalidOutOfAppContent ? Optional.of(DeliveryOutcome.INVALID_CONTENT) : Optional.empty()));
            }
        }
        return planned;
    }

    /** Kênh trong ứng dụng: cổng cho người thân, ứng dụng cho nhân viên và người cao tuổi. */
    public static String inApp(RecipientType type) {
        return type == RecipientType.FAMILY_MEMBER ? "FAMILY_PORTAL" : "IN_APP";
    }

    public static boolean outOfApp(String channel) {
        return channel.equals(SMS) || channel.startsWith(OTHER_PREFIX);
    }

    private static Optional<DeliveryOutcome> sms(NotificationLevel level, Recipient r, boolean invalidContent) {
        if (r.type() == RecipientType.STAFF && level == NotificationLevel.MEDIUM && !r.onShift()) {
            return Optional.of(DeliveryOutcome.SKIPPED_OFF_SHIFT);
        }
        if (invalidContent) {
            return Optional.of(DeliveryOutcome.INVALID_CONTENT);
        }
        if (r.address().filter(a -> !a.isBlank()).isEmpty()) {
            return Optional.of(DeliveryOutcome.NO_ADDRESS);
        }
        return Optional.empty();
    }
}
