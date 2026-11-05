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
import com.eldercare.backend.family.web.request.SubmitFeedbackRequest.FeedbackType;
import com.eldercare.backend.family.web.request.SubmitFeedbackRequest.SenderType;
import com.eldercare.backend.family.web.request.SubmitFeedbackRequest.WishTopic;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

/**
 * Chi tiết phản hồi (api-012 {@code FeedbackDetail}, đổi api 2026-11-05): các trường của {@code Feedback} cùng nguồn,
 * thời điểm trả lời, đóng, các mục, lệnh được phép. Nhân viên có {@code senderHealthRightEffective} để cảnh báo lúc soạn
 * trả lời; người gửi có {@code healthReplyHidden} khi phần trả lời là thông tin sức khỏe mà quyền xem sức khỏe không có
 * tác dụng (FR-058).
 */
// Hợp đồng ghi allOf(Feedback, {chi tiết}); JSON vẫn là một object phẳng
@Schema(
        name = "FeedbackDetail",
        allOf = {FeedbackResponse.class, FeedbackDetailResponse.Detail.class})
public record FeedbackDetailResponse(
        long residentId,
        SenderType senderType,
        Long familyMemberId,
        FeedbackType type,
        ContentGroup group,
        WishTopic wishTopic,
        String content,
        List<Long> fileIds,
        long id,
        Status status,
        Priority priority,
        Instant dueAt,
        boolean overdue,
        String assignee,
        String familyMemberName,
        Instant submittedAt,
        Source source,
        Long assigneeId,
        Instant repliedAt,
        Instant closedAt,
        CloseMethod closeMethod,
        List<Item> items,
        Boolean senderHealthRightEffective,
        Boolean healthReplyHidden,
        List<String> allowedCommands) {

    /** Phần chi tiết thêm vào {@code Feedback}. */
    @Schema(name = "FeedbackDetailPart")
    public record Detail(
            Source source,
            Long assigneeId,
            Instant repliedAt,
            Instant closedAt,
            CloseMethod closeMethod,
            List<Item> items,
            Boolean senderHealthRightEffective,
            Boolean healthReplyHidden,
            List<String> allowedCommands) {}

    /** Mục của phản hồi ({@code FeedbackItem}). */
    @Schema(name = "FeedbackItem")
    public record Item(
            long id, FeedbackItem.Type type, String content, List<Long> fileIds, Long byAccountId, Instant at) {

        static Item of(ItemView v) {
            return new Item(v.id(), v.type(), v.content(), v.fileIds(), v.by(), v.at());
        }
    }

    public static FeedbackDetailResponse of(FeedbackView view) {
        Feedback f = view.feedback();
        FeedbackResponse base = FeedbackResponse.of(view);
        return new FeedbackDetailResponse(
                base.residentId(),
                base.senderType(),
                base.familyMemberId(),
                base.type(),
                base.group(),
                base.wishTopic(),
                base.content(),
                base.fileIds(),
                base.id(),
                base.status(),
                base.priority(),
                base.dueAt(),
                base.overdue(),
                base.assignee(),
                base.familyMemberName(),
                base.submittedAt(),
                f.source(),
                f.assigneeAccountId(),
                f.repliedAt(),
                f.closedAt(),
                f.closeMethod(),
                view.items().stream().map(Item::of).toList(),
                view.senderHealthRightEffective(),
                view.healthReplyHidden(),
                view.allowedCommands());
    }
}
