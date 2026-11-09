package com.eldercare.backend.nutrition.api.events;

import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.notification.ContentPart;
import com.eldercare.backend.common.notification.HasNotificationRequests;
import com.eldercare.backend.common.notification.InformationType;
import com.eldercare.backend.common.notification.NotificationLevel;
import com.eldercare.backend.common.notification.NotificationRequest;
import com.eldercare.backend.common.notification.RecipientGroup;
import com.eldercare.backend.common.notification.SourceGroup;
import com.eldercare.backend.common.notification.SourceRecord;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Người Đang lưu trú chưa gán chế độ ăn (FR-014; FR-065 dòng đầu): báo Bác sĩ mức Nhẹ khi Hoàn tất tiếp nhận mà chưa có
 * bản gán Hiệu lực. Chỉ tới nhân viên; nội dung không nêu chế độ ăn, dị ứng. Lần chốt còn thiếu báo ở phase chốt suất.
 */
public record ResidentWithoutDietAssignment(long residentId, Instant at) implements HasNotificationRequests {

    @Override
    public List<NotificationRequest> notificationRequests() {
        return List.of(new NotificationRequest(
                "M08",
                "RESIDENT_WITHOUT_DIET_ASSIGNMENT",
                SourceGroup.OTHER,
                new SourceRecord("M08.RESIDENT_NUTRITION", String.valueOf(residentId)),
                "M08.RESIDENT_WITHOUT_DIET_ASSIGNMENT:" + residentId + ":" + at.toEpochMilli(),
                Optional.of(NotificationLevel.LOW),
                Optional.of(residentId),
                Optional.empty(),
                List.of(new RecipientGroup.FacilityRole(Role.DOCTOR)),
                List.of(new ContentPart(
                        InformationType.GENERAL,
                        "Người cao tuổi vừa Hoàn tất tiếp nhận chưa được gán chế độ ăn; đang dùng chế độ ăn mặc định")),
                Optional.empty(),
                Optional.of("Chưa gán chế độ ăn"),
                Optional.empty(),
                false,
                at,
                Map.of("residentId", residentId),
                false,
                Optional.empty(),
                Optional.empty()));
    }
}
