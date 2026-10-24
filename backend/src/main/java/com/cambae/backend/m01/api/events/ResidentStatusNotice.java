package com.cambae.backend.m01.api.events;

import com.cambae.backend.common.notification.HasNotificationRequests;
import com.cambae.backend.common.notification.InformationSubject;
import com.cambae.backend.common.notification.NotificationLevel;
import com.cambae.backend.common.notification.NotificationRequest;
import com.cambae.backend.common.notification.Q354Case;
import com.cambae.backend.common.notification.RecipientGroup;
import com.cambae.backend.common.notification.SourceGroup;
import com.cambae.backend.common.notification.SourceRecord;
import com.cambae.backend.m01.api.dto.StatusCommand;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Thông báo Chuyển viện và Ghi nhận qua đời: gửi người liên hệ chính mức Khẩn cấp, nói về người cao tuổi, kèm mã Q-354
 * để 009 gửi cả khi vượt phạm vi bản đồng ý (Q-97, Q-353, Q-354; research H-14; FR-054). Phát cùng
 * {@link ResidentStatusChanged} sau hai lệnh này, tách riêng để sự kiện đổi trạng thái (002, 016, 009 giờ yên tĩnh) không
 * mang yêu cầu thông báo. Khóa sự kiện là dòng lịch sử trạng thái, thời điểm là lúc đổi trạng thái nên tin báo qua đời
 * không bị 009 coi là việc sau qua đời (FR-015).
 *
 * @param command {@code TRANSFER_OUT} hoặc {@code RECORD_DEATH}
 */
public record ResidentStatusNotice(long residentId, StatusCommand command, long historyId, Instant at)
        implements HasNotificationRequests {

    public ResidentStatusNotice {
        if (command != StatusCommand.TRANSFER_OUT && command != StatusCommand.RECORD_DEATH) {
            throw new IllegalArgumentException("Chỉ Chuyển viện và Ghi nhận qua đời gửi thông báo: " + command);
        }
    }

    @Override
    public List<NotificationRequest> notificationRequests() {
        boolean death = command == StatusCommand.RECORD_DEATH;
        String eventType = death ? "RESIDENT_DEATH_RECORDED" : "RESIDENT_TRANSFERRED_OUT";
        String text = death ? "Đã ghi nhận người cao tuổi qua đời." : "Người cao tuổi đã được chuyển viện.";
        return List.of(new NotificationRequest(
                "M01",
                eventType,
                SourceGroup.OTHER,
                new SourceRecord("M01.STATUS_HISTORY", String.valueOf(historyId)),
                eventType + ":" + residentId + ":" + historyId,
                Optional.of(NotificationLevel.URGENT),
                Optional.of(residentId),
                Optional.empty(),
                List.of(new RecipientGroup.PrimaryContact(residentId)),
                Notices.general(text),
                Optional.of(text),
                Optional.of(death ? "Ghi nhận qua đời" : "Chuyển viện"),
                Optional.empty(),
                false,
                at,
                Map.of(),
                false,
                Optional.of(InformationSubject.ABOUT_RESIDENT),
                Optional.of(death ? Q354Case.DEATH : Q354Case.TRANSFER)));
    }
}
