package com.eldercare.backend.roombedallocation.api.events;

import com.eldercare.backend.common.notification.ContentPart;
import com.eldercare.backend.common.notification.InformationType;
import com.eldercare.backend.common.notification.NotificationLevel;
import com.eldercare.backend.common.notification.NotificationRequest;
import com.eldercare.backend.common.notification.SourceGroup;
import com.eldercare.backend.common.notification.SourceRecord;
import com.eldercare.backend.roombedallocation.api.dto.BedNotificationContext;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Dựng khuôn thông báo; không gửi trực tiếp và không tự giải quyết quyền/phân công của người nhận. */
final class BedEventNotifications {
    private BedEventNotifications() {}

    static List<NotificationRequest> create(
            String event,
            long id,
            Long residentId,
            String sourceType,
            NotificationLevel level,
            InformationType information,
            String cycle,
            BedNotificationContext context) {
        String title = "Thông tin phòng, giường cần xử lý";
        return List.of(new NotificationRequest(
                "M03",
                event,
                SourceGroup.OTHER,
                new SourceRecord("M03." + sourceType, Long.toString(id)),
                event + ":" + id + ":" + digest(cycle) + ":" + context.occurredAt(),
                Optional.of(level),
                Optional.ofNullable(residentId),
                Optional.ofNullable(context.floorAreaId()),
                context.recipients(),
                List.of(new ContentPart(information, title)),
                Optional.of(title),
                Optional.of(title),
                Optional.empty(),
                false,
                context.occurredAt(),
                Map.of(),
                false,
                Optional.empty(),
                Optional.empty()));
    }

    private static String digest(String cycle) {
        try {
            return HexFormat.of()
                    .formatHex(MessageDigest.getInstance("SHA-256").digest(cycle.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("JDK thiếu thuật toán SHA-256", exception);
        }
    }
}
