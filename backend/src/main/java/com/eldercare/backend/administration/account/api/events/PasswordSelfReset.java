package com.eldercare.backend.administration.account.api.events;

import com.eldercare.backend.common.notification.HasNotificationRequests;
import com.eldercare.backend.common.notification.NotificationRequest;
import com.eldercare.backend.common.notification.RecipientGroup;
import com.eldercare.backend.common.notification.SourceGroup;
import com.eldercare.backend.common.notification.SourceRecord;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Chủ tài khoản tự đặt lại mật khẩu bằng mã qua tin nhắn (002 T105; FR-052a, BR-M15-25): báo chủ tài khoản; tài khoản
 * có vai trò Quản lý viện thì báo thêm các Quản lý viện khác (người đặt lại bị loại khỏi nhóm nhờ {@code causedBy}).
 *
 * @param staffId hồ sơ nhân viên của tài khoản nhân viên, {@code null} với người thân
 * @param familyMemberId hồ sơ người thân của tài khoản người thân, {@code null} với nhân viên
 */
public record PasswordSelfReset(long accountId, Long staffId, Long familyMemberId, boolean manager, Instant at)
        implements HasNotificationRequests {

    private static final String TEXT =
            "Mật khẩu tài khoản vừa được đặt lại bằng mã gửi qua tin nhắn. Nếu không phải bạn"
                    + " thực hiện, hãy liên hệ viện ngay.";

    @Override
    public List<NotificationRequest> notificationRequests() {
        SourceRecord source = new SourceRecord("M15.ACCOUNT", String.valueOf(accountId));
        RecipientGroup owner = staffId != null
                ? new RecipientGroup.Staff(staffId, "ACCOUNT_OWNER")
                : new RecipientGroup.FamilyMember(familyMemberId, "ACCOUNT_OWNER");
        List<NotificationRequest> requests = new ArrayList<>();
        requests.add(Notices.of(
                "PASSWORD_SELF_RESET",
                SourceGroup.OTHER,
                source,
                "PASSWORD_SELF_RESET:" + accountId + ":" + at,
                null,
                owner,
                "Mật khẩu đã được đặt lại",
                TEXT,
                at));
        if (manager) {
            NotificationRequest managers = Notices.of(
                    "PASSWORD_SELF_RESET_MANAGER",
                    SourceGroup.OTHER,
                    source,
                    "PASSWORD_SELF_RESET_MANAGER:" + accountId + ":" + at,
                    null,
                    Notices.MANAGERS,
                    "Quản lý viện đặt lại mật khẩu",
                    "Một tài khoản Quản lý viện vừa tự đặt lại mật khẩu bằng mã gửi qua tin nhắn.",
                    at);
            requests.add(causedBy(managers, accountId));
        }
        return requests;
    }

    /** Người đặt lại không nhận thông báo của nhóm Quản lý viện (009 bỏ {@code causedBy} khi mức Nhẹ). */
    private static NotificationRequest causedBy(NotificationRequest r, long accountId) {
        return new NotificationRequest(
                r.sourceModule(),
                r.eventType(),
                r.sourceGroup(),
                r.sourceRecord(),
                r.eventKey(),
                r.level(),
                r.residentId(),
                r.floorAreaId(),
                r.recipientGroups(),
                r.content(),
                r.shortContent(),
                r.title(),
                Optional.of(accountId),
                false,
                r.occurredAt(),
                r.displayParams(),
                r.afterDeath(),
                r.informationSubject(),
                r.q354Case());
    }
}
