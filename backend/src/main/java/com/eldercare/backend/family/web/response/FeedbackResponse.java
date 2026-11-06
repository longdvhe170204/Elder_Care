package com.eldercare.backend.family.web.response;

import com.eldercare.backend.family.internal.feedback.domain.Feedback;
import com.eldercare.backend.family.internal.feedback.domain.Feedback.ContentGroup;
import com.eldercare.backend.family.internal.feedback.domain.Feedback.Priority;
import com.eldercare.backend.family.internal.feedback.domain.Feedback.Status;
import com.eldercare.backend.family.internal.feedback.service.FeedbackService.FeedbackView;
import com.eldercare.backend.family.web.request.SubmitFeedbackRequest;
import com.eldercare.backend.family.web.request.SubmitFeedbackRequest.FeedbackType;
import com.eldercare.backend.family.web.request.SubmitFeedbackRequest.SenderType;
import com.eldercare.backend.family.web.request.SubmitFeedbackRequest.WishTopic;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

// Hợp đồng ghi allOf(SubmitFeedback, {trạng thái}); JSON vẫn là một object phẳng
@Schema(
        name = "Feedback",
        allOf = {SubmitFeedbackRequest.class, FeedbackResponse.State.class})
public record FeedbackResponse(
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
        Instant submittedAt) {

    /** Phần trạng thái của phản hồi (data-model mục 9); tên người gửi, thời điểm gửi bổ sung 2026-11-05. */
    @Schema(name = "FeedbackState")
    public record State(
            long id,
            Status status,
            Priority priority,
            Instant dueAt,
            boolean overdue,
            String assignee,

            @Schema(accessMode = Schema.AccessMode.READ_ONLY)
            String familyMemberName,

            @Schema(accessMode = Schema.AccessMode.READ_ONLY)
            Instant submittedAt) {}

    public static FeedbackResponse of(FeedbackView view) {
        Feedback f = view.feedback();
        return new FeedbackResponse(
                f.residentId(),
                SenderType.valueOf(f.senderType().name()),
                f.familyMemberId(),
                FeedbackType.valueOf(f.type().name()),
                f.group(),
                f.wishTopic() == null ? null : WishTopic.valueOf(f.wishTopic().name()),
                view.content(),
                view.fileIds(),
                f.id(),
                f.status(),
                f.priority(),
                f.dueAt(),
                view.overdue(),
                view.assignee(),
                view.familyMemberName(),
                f.submittedAt());
    }
}
