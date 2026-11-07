package com.eldercare.backend.family.api.events;

import com.eldercare.backend.common.notification.ContentPart;
import com.eldercare.backend.common.notification.HasNotificationRequests;
import com.eldercare.backend.common.notification.InformationType;
import com.eldercare.backend.common.notification.NotificationLevel;
import com.eldercare.backend.common.notification.NotificationRequest;
import com.eldercare.backend.common.notification.RecipientGroup;
import com.eldercare.backend.common.notification.SourceGroup;
import com.eldercare.backend.common.notification.SourceRecord;
import com.eldercare.backend.common.notification.Substitution;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Lần chặn đón (FR-023): Trưởng tầng của tầng người cao tuổi nhận thông báo Nhẹ. Không biết tầng (người cao tuổi chưa có
 * vị trí ngày đó) thì không có người nhận.
 *
 * @param reason lý do chặn ({@code NOT_ON_LIST}, {@code ID_DOCUMENT_MISMATCH}, {@code EXCEPTION_EXPIRED},
 *     {@code SELF_RETURN_OFF})
 */
public record PickupBlockNotice(long blockId, long residentId, Optional<Long> floorAreaId, String reason, Instant at)
        implements HasNotificationRequests {

    public static final String SOURCE_TYPE = "M10.PICKUP_BLOCK";
    private static final String TITLE = "Lượt đón bị chặn";

    @Override
    public List<NotificationRequest> notificationRequests() {
        if (floorAreaId.isEmpty()) {
            return List.of();
        }
        String summary = TITLE + ": "
                + switch (reason) {
                    case "NOT_ON_LIST" -> "người đón không thuộc danh sách được phép đón";
                    case "ID_DOCUMENT_MISMATCH" -> "giấy tờ không khớp số đã đăng ký";
                    case "EXCEPTION_EXPIRED" -> "ngoại lệ đón đã hết hạn";
                    default -> "người cao tuổi không có dấu được tự về";
                };
        return List.of(new NotificationRequest(
                "M10",
                "PICKUP_BLOCKED",
                SourceGroup.FAMILY_INFO,
                new SourceRecord(SOURCE_TYPE, String.valueOf(blockId)),
                SOURCE_TYPE + ":" + blockId,
                Optional.of(NotificationLevel.LOW),
                Optional.of(residentId),
                floorAreaId,
                List.of(new RecipientGroup.FloorManager(floorAreaId.get(), new Substitution.Default())),
                List.of(new ContentPart(InformationType.GENERAL, summary)),
                Optional.of(summary),
                Optional.of(TITLE),
                Optional.empty(),
                false,
                at,
                Map.of(),
                false,
                Optional.empty(),
                Optional.empty()));
    }
}
