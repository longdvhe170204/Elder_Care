package com.eldercare.backend.belongings.internal.belonging.domain;

import com.eldercare.backend.belongings.api.events.BelongingNotice;
import com.eldercare.backend.common.notification.ContentPart;
import com.eldercare.backend.common.notification.InformationSubject;
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
 * Dựng yêu cầu thông báo của M12 (FR-032, research K-13): nội dung chỉ loại sự kiện và loại đồ (SC-012); có nhóm người
 * thân thì khai "về việc của chính người thân".
 */
public final class BelongingNotices {

    private BelongingNotices() {}

    /**
     * Tiêu đề theo sự kiện (test luồng BF-14 2026-11-23; trước là "Đồ gửi" cho mọi thông báo). Không chứa tên người hay
     * sức khỏe (009 FR-034); sự kiện chưa có ở đây dùng "Đồ gửi".
     */
    private static final Map<String, String> TITLES = Map.ofEntries(
            Map.entry("BELONGING_VALUABLE_INTAKE", "Viện đã tiếp nhận đồ gửi"),
            Map.entry("BELONGING_LOST", "Đồ gửi thất lạc"),
            Map.entry("BELONGING_DAMAGED", "Đồ gửi hư hỏng"),
            Map.entry("BELONGING_FOUND", "Đã tìm thấy đồ gửi"),
            Map.entry("BELONGING_RETURNED", "Đồ gửi đã được trả"),
            Map.entry("BELONGING_DISPOSED", "Đồ gửi đã được xử lý"),
            Map.entry("BELONGING_HANDOVER_VOIDED", "Bản ghi đồ gửi bị hủy ghi nhận"),
            Map.entry("BELONGING_INCIDENT_LINK_VOIDED", "Liên kết sự cố của đồ gửi bị hủy"),
            Map.entry("BELONGING_HANDOVER_HOLDER_ABSENT", "Đồ bạn giữ đã được bàn giao thay"),
            Map.entry("BELONGING_HELD_AT_SHIFT_END", "Còn giữ đồ có giá trị khi hết ca"),
            Map.entry("BELONGINGS_AT_PREVIOUS_FLOOR", "Đồ gửi còn ở tủ tầng cũ"),
            Map.entry("BELONGING_UNRETURNED_AFTER_FINAL", "Đồ gửi chưa trả sau trạng thái cuối"),
            Map.entry("BELONGING_UNRETURNED_AT_ENDING", "Còn đồ gửi chưa trả"),
            Map.entry("BELONGING_DISPOSAL_ELIGIBLE", "Đồ gửi đủ thời gian chờ xử lý"),
            Map.entry("FAMILY_ID_DOCUMENT_MISSING", "Người nhận đồ chưa có giấy tờ"),
            Map.entry("SELF_CUSTODY_CONSENT_RECORDED", "Đã ghi đồng ý cho tự giữ"),
            Map.entry("SELF_CUSTODY_CONSENT_WITHDRAWN", "Đồng ý cho tự giữ đã bị rút"),
            Map.entry("SELF_CUSTODY_CONSENT_WITHDRAWN_IN_USE", "Cần thu lại đồ tự giữ"),
            Map.entry("SELF_CUSTODY_CONSENT_REQUIRED_IN_USE", "Cần thu lại đồ tự giữ"),
            Map.entry("RECIPIENT_CONFIRMATION_REQUESTED", "Cần xác nhận người nhận đồ"),
            Map.entry("RECIPIENT_CONFIRMATION_CONFIRMED", "Xác nhận người nhận đồ có hiệu lực"),
            Map.entry("RECIPIENT_CONFIRMATION_APPROVED_ON_BEHALF", "Xác nhận người nhận đồ có hiệu lực"),
            Map.entry("RECIPIENT_CONFIRMATION_REJECTED", "Người đại diện từ chối người nhận đồ"),
            Map.entry("RECIPIENT_CONFIRMATION_EXPIRED", "Xác nhận người nhận đồ hết hạn"),
            Map.entry("RECIPIENT_CONFIRMATION_CANCELLED_NO_ITEMS", "Xác nhận người nhận đồ tự hủy"),
            Map.entry("RECIPIENT_CONFIRMATION_ELIGIBLE", "Có thể duyệt thay xác nhận người nhận đồ"),
            Map.entry("INVENTORY_PERIOD_STARTED", "Kỳ kiểm kê đồ gửi"),
            Map.entry("INVENTORY_OVERDUE", "Quá hạn kiểm kê đồ gửi"),
            Map.entry("INVENTORY_OVERDUE_MANAGER", "Hết hạn kiểm kê đồ gửi"),
            Map.entry("INVENTORY_UNKNOWN_OWNER_ITEMS", "Kiểm kê có đồ không rõ chủ"));

    static String titleOf(String eventType) {
        return TITLES.getOrDefault(eventType, "Đồ gửi");
    }

    public static BelongingNotice notice(
            String eventType,
            String sourceType,
            long sourceId,
            NotificationLevel level,
            long residentId,
            List<RecipientGroup> recipients,
            String text,
            Long causedBy,
            Instant at) {
        return notice(
                eventType,
                sourceType,
                sourceId,
                eventType + ":" + sourceId,
                level,
                residentId,
                recipients,
                text,
                causedBy,
                at,
                false);
    }

    /**
     * Như trên, với khóa sự kiện riêng (một nguồn có nhiều lần nhắc, ví dụ mỗi người giữ của một ca), người cao tuổi có thể
     * trống, dấu "sau qua đời" (009 FR-015).
     */
    public static BelongingNotice notice(
            String eventType,
            String sourceType,
            long sourceId,
            String eventKey,
            NotificationLevel level,
            Long residentId,
            List<RecipientGroup> recipients,
            String text,
            Long causedBy,
            Instant at,
            boolean afterDeath) {
        boolean toFamily = recipients.stream()
                .anyMatch(g -> g instanceof RecipientGroup.Representative
                        || g instanceof RecipientGroup.PrimaryContact
                        || g instanceof RecipientGroup.FamilyMember
                        || g instanceof RecipientGroup.FamilyWithRight);
        return new BelongingNotice(List.of(new NotificationRequest(
                "M12",
                eventType,
                SourceGroup.OTHER,
                new SourceRecord(sourceType, Long.toString(sourceId)),
                eventKey,
                Optional.of(level),
                Optional.ofNullable(residentId),
                Optional.empty(),
                recipients,
                List.of(new ContentPart(InformationType.GENERAL, text)),
                Optional.of(text),
                Optional.of(titleOf(eventType)),
                Optional.ofNullable(causedBy),
                false,
                at,
                Map.of(),
                afterDeath,
                toFamily ? Optional.of(InformationSubject.ABOUT_FAMILY_MATTER) : Optional.empty(),
                Optional.empty())));
    }

    /**
     * Thêm chi tiết (người cao tuổi, đồ, vị trí) vào nội dung đầy đủ của yêu cầu chỉ gửi nhân viên (test luồng BF-14
     * 2026-11-23). Yêu cầu có người thân giữ nguyên (SC-012, Q-151); nội dung rút gọn cho kênh ngoài ứng dụng giữ nguyên
     * (009 FR-019).
     */
    public static BelongingNotice withStaffDetail(BelongingNotice notice, String detail) {
        if (detail == null || detail.isBlank()) {
            return notice;
        }
        return new BelongingNotice(notice.notificationRequests().stream()
                .map(r -> r.informationSubject().isPresent()
                        ? r
                        : new NotificationRequest(
                                r.sourceModule(),
                                r.eventType(),
                                r.sourceGroup(),
                                r.sourceRecord(),
                                r.eventKey(),
                                r.level(),
                                r.residentId(),
                                r.floorAreaId(),
                                r.recipientGroups(),
                                r.content().stream()
                                        .map(c -> new ContentPart(c.type(), c.content() + ". " + detail))
                                        .toList(),
                                r.shortContent(),
                                r.title(),
                                r.causedBy(),
                                r.notifyCauser(),
                                r.occurredAt(),
                                r.displayParams(),
                                r.afterDeath(),
                                r.informationSubject(),
                                r.q354Case()))
                .toList());
    }
}
