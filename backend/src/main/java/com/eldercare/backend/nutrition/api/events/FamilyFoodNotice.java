package com.eldercare.backend.nutrition.api.events;

import com.eldercare.backend.common.notification.ContentPart;
import com.eldercare.backend.common.notification.HasNotificationRequests;
import com.eldercare.backend.common.notification.InformationSubject;
import com.eldercare.backend.common.notification.InformationType;
import com.eldercare.backend.common.notification.NotificationLevel;
import com.eldercare.backend.common.notification.NotificationRequest;
import com.eldercare.backend.common.notification.RecipientGroup;
import com.eldercare.backend.common.notification.SourceGroup;
import com.eldercare.backend.common.notification.SourceRecord;
import com.eldercare.backend.common.notification.Substitution;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Thông báo của đồ ăn gia đình (FR-065, FR-055, FR-056, FR-057):
 *
 * <ul>
 *   <li>{@code SENDER_NOT_ALLOWED}: đồ ăn chuyển Không sử dụng, người gửi là người thân có tài khoản và quan hệ Hiệu lực, mức
 *       Nhẹ; phần "chung" nêu cách nhận lại, lý do liên quan dị ứng, chế độ ăn ở phần "sức khỏe" ({@code healthReason});
 *   <li>{@code NURSE_CONFLICT}: đồ ăn đang dùng chuyển Không sử dụng vì dị ứng hoặc chế độ ăn mới, Điều dưỡng phụ trách, mức
 *       Trung bình;
 *   <li>{@code NURSE_EXPIRED}: đồ ăn đang dùng tới hạn dùng, Điều dưỡng phụ trách, mức Nhẹ.
 * </ul>
 */
public record FamilyFoodNotice(
        long foodId, long residentId, Kind kind, Long senderFamilyMemberId, String healthReason, Instant at)
        implements HasNotificationRequests {

    public enum Kind {
        SENDER_NOT_ALLOWED,
        NURSE_CONFLICT,
        NURSE_EXPIRED
    }

    @Override
    public List<NotificationRequest> notificationRequests() {
        return switch (kind) {
            case SENDER_NOT_ALLOWED -> sender();
            case NURSE_CONFLICT ->
                List.of(nurse(
                        "FAMILY_FOOD_CONFLICT",
                        NotificationLevel.MEDIUM,
                        "Đồ ăn gia đình đang dùng chuyển Không sử dụng do dị ứng hoặc chế độ ăn mới của người cao tuổi",
                        "Đồ ăn gia đình không còn được sử dụng"));
            case NURSE_EXPIRED ->
                List.of(nurse(
                        "FAMILY_FOOD_EXPIRED",
                        NotificationLevel.LOW,
                        "Đồ ăn gia đình đang dùng đã tới hạn dùng và chuyển Không sử dụng",
                        "Đồ ăn gia đình hết hạn dùng"));
        };
    }

    private List<NotificationRequest> sender() {
        if (senderFamilyMemberId == null) {
            return List.of();
        }
        List<ContentPart> content = new ArrayList<>();
        content.add(new ContentPart(
                InformationType.GENERAL,
                "Đồ ăn bạn gửi không được sử dụng theo quy định của viện; vui lòng liên hệ viện để nhận lại"));
        if (healthReason != null && !healthReason.isBlank()) {
            content.add(new ContentPart(InformationType.HEALTH, "Lý do: " + healthReason));
        }
        return List.of(request(
                "FAMILY_FOOD_NOT_ALLOWED",
                "M08.FAMILY_FOOD_NOT_ALLOWED:" + foodId,
                NotificationLevel.LOW,
                List.of(new RecipientGroup.FamilyMember(senderFamilyMemberId, "FAMILY_FOOD_SENDER")),
                content,
                "Đồ ăn gửi cho người thân không được sử dụng",
                Optional.of(InformationSubject.ABOUT_FAMILY_MATTER)));
    }

    private NotificationRequest nurse(String eventType, NotificationLevel level, String text, String title) {
        return request(
                eventType,
                "M08." + eventType + ":" + foodId,
                level,
                List.of(new RecipientGroup.AssignedNurse(residentId, new Substitution.Default())),
                List.of(new ContentPart(InformationType.GENERAL, text)),
                title,
                Optional.empty());
    }

    private NotificationRequest request(
            String eventType,
            String eventKey,
            NotificationLevel level,
            List<RecipientGroup> recipients,
            List<ContentPart> content,
            String title,
            Optional<InformationSubject> subject) {
        return new NotificationRequest(
                "M08",
                eventType,
                SourceGroup.OTHER,
                new SourceRecord("M08.FAMILY_FOOD", String.valueOf(foodId)),
                eventKey,
                Optional.of(level),
                Optional.of(residentId),
                Optional.empty(),
                recipients,
                content,
                Optional.empty(),
                Optional.of(title),
                Optional.empty(),
                false,
                at,
                Map.of("foodId", foodId),
                false,
                subject,
                Optional.empty());
    }
}
