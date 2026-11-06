package com.eldercare.backend.family.web.request;

import com.eldercare.backend.family.internal.feedback.domain.Feedback.ContentGroup;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Thân gửi phản hồi (api-012 {@code SubmitFeedback}; FR-058): người thân gửi cho chính mình, Hành chính ghi thay chọn
 * {@code familyMemberId}. {@code senderType = RESIDENT}, loại {@code WISH}, {@code wishTopic} thuộc US9 (FR-058a, FR-058b).
 */
@Schema(name = "SubmitFeedback")
public record SubmitFeedbackRequest(
        @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        Long residentId,

        @Schema(defaultValue = "FAMILY_MEMBER") SenderType senderType,
        Long familyMemberId,

        @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        FeedbackType type,

        @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        ContentGroup group,

        WishTopic wishTopic,

        @NotNull @Size(max = 4000) @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        String content,

        @Size(max = 10) List<Long> fileIds) {

    public enum SenderType {
        FAMILY_MEMBER,
        RESIDENT
    }

    public enum FeedbackType {
        SUGGESTION,
        COMPLAINT,
        QUESTION,
        WISH
    }

    public enum WishTopic {
        VISITORS_PICKERS,
        OTHER
    }
}
