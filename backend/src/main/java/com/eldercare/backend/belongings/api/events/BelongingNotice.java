package com.eldercare.backend.belongings.api.events;

import com.eldercare.backend.common.notification.HasNotificationRequests;
import com.eldercare.backend.common.notification.InformationSubject;
import com.eldercare.backend.common.notification.NotificationRequest;
import com.eldercare.backend.common.notification.RecipientGroup;
import java.util.List;
import java.util.Optional;

/**
 * Thông báo của M12 ({@code CoYeuCauThongBao}; FR-032). Mọi yêu cầu có nhóm người thân khai đối tượng "về việc của chính
 * người thân" và không mang mã Q-354 (research K-13, 009 FR-001a, Q-362); vi phạm thì không dựng được sự kiện.
 */
public record BelongingNotice(List<NotificationRequest> notificationRequests) implements HasNotificationRequests {

    public BelongingNotice {
        notificationRequests = List.copyOf(notificationRequests);
        for (NotificationRequest request : notificationRequests) {
            boolean toFamily = request.recipientGroups().stream().anyMatch(BelongingNotice::isFamily);
            if (toFamily
                    && (!request.informationSubject().equals(Optional.of(InformationSubject.ABOUT_FAMILY_MATTER))
                            || request.q354Case().isPresent())) {
                throw new IllegalArgumentException(
                        "Thông báo đồ gửi tới người thân phải là ABOUT_FAMILY_MATTER, không mã Q-354: "
                                + request.eventType());
            }
        }
    }

    private static boolean isFamily(RecipientGroup group) {
        return group instanceof RecipientGroup.FamilyMember
                || group instanceof RecipientGroup.PrimaryContact
                || group instanceof RecipientGroup.Representative
                || group instanceof RecipientGroup.FamilyWithRight;
    }
}
