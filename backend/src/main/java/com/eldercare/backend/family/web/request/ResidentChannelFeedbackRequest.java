package com.eldercare.backend.family.web.request;

import com.eldercare.backend.family.internal.feedback.domain.Feedback.ContentGroup;
import com.eldercare.backend.family.web.request.SubmitFeedbackRequest.FeedbackType;
import com.eldercare.backend.family.web.request.SubmitFeedbackRequest.WishTopic;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Người cao tuổi gửi phản hồi trên kênh của mình (api-012 {@code POST /resident-channel/feedback}; FR-058, FR-058a,
 * FR-058b): người cao tuổi lấy từ tài khoản đăng nhập; {@code wishTopic} bắt buộc khi loại Mong muốn.
 */
public record ResidentChannelFeedbackRequest(
        @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        FeedbackType type,

        @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        ContentGroup group,

        WishTopic wishTopic,

        @NotNull @Size(max = 4000) @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        String content) {}
