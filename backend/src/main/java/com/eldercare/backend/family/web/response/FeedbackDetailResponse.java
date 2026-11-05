package com.eldercare.backend.family.web.response;

import com.eldercare.backend.family.internal.feedback.domain.Feedback;
import com.eldercare.backend.family.internal.feedback.domain.Feedback.CloseMethod;
import com.eldercare.backend.family.internal.feedback.domain.Feedback.ContentGroup;
import com.eldercare.backend.family.internal.feedback.domain.Feedback.Priority;
import com.eldercare.backend.family.internal.feedback.domain.Feedback.Source;
import com.eldercare.backend.family.internal.feedback.domain.Feedback.Status;
import com.eldercare.backend.family.internal.feedback.domain.FeedbackItem;
import com.eldercare.backend.family.internal.feedback.service.FeedbackService.FeedbackView;
import com.eldercare.backend.family.internal.feedback.service.FeedbackService.ItemView;
import java.time.Instant;
import java.util.List;

/**
 * Chi tiết phản hồi (hợp đồng {@code GET /feedback/{id}} không khai nội dung): các trường của {@code Feedback} cùng nguồn,
 * thời điểm gửi, đóng, các mục. Nhân viên có {@code senderHealthRightEffective} để cảnh báo lúc soạn trả lời; người gửi có
 * {@code healthReplyHidden} khi phần trả lời là thông tin sức khỏe mà quyền xem sức khỏe không có tác dụng (FR-058).
 */
public record FeedbackDetailResponse(
        long id,
        long residentId,
        String senderType,
        long familyMemberId,
        String type,
        ContentGroup group,
        String content,
        List<Long> fileIds,
        Source source,
        Instant submittedAt,
        Status status,
        Priority priority,
        Instant dueAt,
        boolean overdue,
        String assignee,
        Long assigneeId,
        Instant repliedAt,
        Instant closedAt,
        CloseMethod closeMethod,
        List<Item> items,
        Boolean senderHealthRightEffective,
        Boolean healthReplyHidden) {

    public record Item(
            long id, FeedbackItem.Type type, String content, List<Long> fileIds, Long byAccountId, Instant at) {

        static Item of(ItemView v) {
            return new Item(v.id(), v.type(), v.content(), v.fileIds(), v.by(), v.at());
        }
    }

    public static FeedbackDetailResponse of(FeedbackView view) {
        Feedback f = view.feedback();
        return new FeedbackDetailResponse(
                f.id(),
                f.residentId(),
                "FAMILY_MEMBER",
                f.familyMemberId(),
                f.type().name(),
                f.group(),
                view.content(),
                view.fileIds(),
                f.source(),
                f.submittedAt(),
                f.status(),
                f.priority(),
                f.dueAt(),
                view.overdue(),
                view.assignee(),
                f.assigneeAccountId(),
                f.repliedAt(),
                f.closedAt(),
                f.closeMethod(),
                view.items().stream().map(Item::of).toList(),
                view.senderHealthRightEffective(),
                view.healthReplyHidden());
    }
}
