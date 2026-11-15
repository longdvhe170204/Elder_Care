package com.eldercare.backend.family.web.request;

import com.eldercare.backend.common.Reason;
import com.eldercare.backend.family.internal.feedback.domain.Feedback.Priority;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Thân lệnh trên phản hồi (api-012 {@code /feedback/{id}/{command}}; FR-066): lý do (giao lại, đổi mức, mở lại), hướng
 * xử lý và nội dung (trả lời, ghi chú), người được giao, mức mới, tệp. {@code newType}, {@code opinion} thuộc US9.
 */
public record FeedbackCommandRequest(
        Reason reason,
        @Size(max = 4000) String handlingDirection,
        @Size(max = 4000) String content,
        Long assigneeId,
        Priority priority,
        NewType newType,
        Opinion opinion,
        @Size(max = 10) List<Long> fileIds) {

    public static final FeedbackCommandRequest EMPTY =
            new FeedbackCommandRequest(null, null, null, null, null, null, null, null);

    public enum NewType {
        SUGGESTION,
        QUESTION
    }

    /** Mục ý kiến gắn vào phản hồi Mong muốn (BR-M10-12, US9). */
    public record Opinion(
            ItemType itemType,
            Long itemId,
            Long interestGroupId,
            @Pattern(regexp = "LIKE|DISLIKE") String level,
            @Size(max = 1000) String description,
            Boolean confirmReplacement) {

        /** {@code INTEREST}: mục sở thích mới ở 014 (bổ sung 2026-11-10, A duyệt). */
        public enum ItemType {
            VISIT_PICKUP_WISH,
            INTEREST
        }
    }
}
