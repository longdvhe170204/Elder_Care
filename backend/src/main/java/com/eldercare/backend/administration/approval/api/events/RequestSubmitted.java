package com.eldercare.backend.administration.approval.api.events;

import com.eldercare.backend.administration.approval.api.dto.ApprovalRequestView;
import com.eldercare.backend.common.notification.HasNotificationRequests;
import com.eldercare.backend.common.notification.NotificationRequest;
import com.eldercare.backend.common.notification.RecipientGroup;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Đã gửi duyệt ({@code YeuCauDaGuiDuyet}); {@code approvers} đã lọc theo giới hạn vai trò duyệt (contracts mục 1.5).
 * {@code summary} do loại yêu cầu mô tả (người cao tuổi, hồ sơ, nội dung; {@code RequestTypeHandler#submittedNoticeSummary}),
 * chỉ ở nội dung đầy đủ; nội dung rút gọn chỉ nêu mã yêu cầu.
 */
public record RequestSubmitted(ApprovalRequestView request, List<Long> approvers, Optional<String> summary, Instant at)
        implements HasNotificationRequests {

    @Override
    public List<NotificationRequest> notificationRequests() {
        if (approvers.isEmpty()) {
            return List.of();
        }
        String brief = "Yêu cầu " + request.displayCode() + " đang chờ duyệt.";
        return List.of(Notices.of(
                request,
                "REQUEST_SUBMITTED",
                List.of(new RecipientGroup.Approvers(request.type(), approvers)),
                summary.map(s -> s + " " + brief).orElse(brief),
                brief,
                at));
    }
}
