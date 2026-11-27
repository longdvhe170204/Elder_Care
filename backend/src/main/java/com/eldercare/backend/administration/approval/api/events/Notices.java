package com.eldercare.backend.administration.approval.api.events;

import com.eldercare.backend.administration.approval.api.dto.ApprovalRequestView;
import com.eldercare.backend.common.notification.ContentPart;
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
 * Yêu cầu thông báo của sự kiện yêu cầu phê duyệt (009 contracts mục 1.1). Mức theo bảng mức của 009 (dòng 000): có yêu
 * cầu mới Nhẹ; chờ duyệt quá CFG-M15-05, CFG-M15-06 và Áp dụng không thành Trung bình (sửa 2026-11-23, trước để trống nên
 * 009 gắn dấu thiếu mức); nội dung chỉ là thông tin chung, không có sức khỏe.
 */
final class Notices {

    static final String SOURCE_TYPE = "M15.APPROVAL_REQUEST";

    // Demo 2026-11-26 (E): tiêu đề là tên chức năng (cùng nhãn web approvalStatus.js); loại chưa có nhãn giữ "Yêu cầu"
    // + mã
    private static final Map<String, String> TYPE_TITLES = Map.ofEntries(
            Map.entry("M15.CORRECTION", "Đính chính"),
            Map.entry("M02.RESIDENCY_CHANGE", "Điều chỉnh hợp đồng"),
            Map.entry("M02.CONTRACT_TERMS_APPROVAL", "Duyệt điều khoản hợp đồng"),
            Map.entry("M02.ENDING_EXCEPTION", "Ngoại lệ kết thúc lưu trú"),
            Map.entry("M03.ASSET_DISPOSAL", "Thanh lý tài sản"),
            Map.entry("M03.NHAP_BU_PHAN_BO", "Nhập bù phân bổ giường"),
            Map.entry("M09.LEAVE_REQUEST", "Nghỉ đột xuất"),
            Map.entry("M09.SHIFT_SWAP", "Đổi ca"),
            Map.entry("M12.UNCLAIMED_BELONGING_DISPOSAL", "Xử lý đồ không người nhận"),
            Map.entry("M15.RESIDENT_ACCOUNT_LOCK", "Khóa tài khoản người cao tuổi"),
            Map.entry("M11.BALANCE_TRANSACTION", "Giao dịch số dư"));
    private static final Map<String, String> BALANCE_TITLES =
            Map.of("REFUND", "Hoàn số dư", "DEPOSIT_REFUND", "Hoàn tiền cọc", "ADJUSTMENT", "Điều chỉnh số dư");

    private Notices() {}

    static String titleOf(ApprovalRequestView request) {
        if ("M11.BALANCE_TRANSACTION".equals(request.type()) && request.content() != null) {
            String title = BALANCE_TITLES.get(request.content().path("type").asString(""));
            if (title != null) return title;
        }
        return TYPE_TITLES.getOrDefault(request.type(), "Yêu cầu " + request.displayCode());
    }

    private static NotificationLevel levelOf(String eventType) {
        return switch (eventType) {
            case "REQUEST_REMINDER", "REQUEST_ESCALATION", "REQUEST_APPLY_FAILED" -> NotificationLevel.MEDIUM;
            default -> NotificationLevel.LOW;
        };
    }

    static NotificationRequest of(
            ApprovalRequestView request, String eventType, List<RecipientGroup> recipients, String text, Instant at) {
        return of(request, eventType, recipients, text, text, at);
    }

    /** {@code text} là nội dung đầy đủ; {@code shortText} là nội dung rút gọn (kênh ngoài ứng dụng). */
    static NotificationRequest of(
            ApprovalRequestView request,
            String eventType,
            List<RecipientGroup> recipients,
            String text,
            String shortText,
            Instant at) {
        return new NotificationRequest(
                "M15",
                eventType,
                SourceGroup.OTHER,
                new SourceRecord(SOURCE_TYPE, String.valueOf(request.id())),
                eventType + ":" + request.id(),
                Optional.of(levelOf(eventType)),
                Optional.ofNullable(request.target().residentId()),
                Optional.ofNullable(request.target().floorAreaId()),
                recipients,
                List.of(new ContentPart(InformationType.GENERAL, text)),
                Optional.of(shortText),
                Optional.of(titleOf(request)),
                Optional.empty(),
                false,
                at,
                Map.of("displayCode", request.displayCode(), "type", request.type()),
                false,
                Optional.empty(),
                Optional.empty());
    }
}
