package com.eldercare.backend.staffing.handover.web.request;

import com.eldercare.backend.staffing.handover.internal.domain.HandoverItemInput;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;

/** Chỉ nhận trường ghi; trạng thái, phần và tác giả do máy chủ xác định. */
public final class HandoverRequests {
    private HandoverRequests() {}

    public record Item(
            @NotBlank String group,
            String sourceType,
            String sourceId,
            Long residentId,
            Long roomId,
            @NotBlank String description,
            String note,
            Boolean resolved) {
        public HandoverItemInput input() {
            return new HandoverItemInput(
                    group, sourceType, sourceId, residentId, roomId, description, note, Boolean.TRUE.equals(resolved));
        }
    }

    public record ResidentAssessment(Long residentId, String content) {}

    public record Assessments(String general, List<@Valid ResidentAssessment> byResident) {
        public Map<Long, String> residents() {
            var result = new LinkedHashMap<Long, String>();
            if (byResident != null)
                for (var r : byResident) {
                    if (r.residentId() == null
                            || r.content() == null
                            || result.putIfAbsent(r.residentId(), r.content()) != null)
                        throw new com.eldercare.backend.common.error.BusinessException(
                                org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY,
                                "INVALID_VALUE",
                                "Nhận định cần người cao tuổi và nội dung, không trùng người");
                }
            return result;
        }
    }

    @Schema(name = "HandoverCommentRequest")
    public record Comment(@NotBlank @Schema(minLength = 1) String content) {}

    public record Reply(@NotNull Long commentId, @NotNull String content) {}

    public record Resubmit(@NotNull List<@Valid Reply> replies, List<@Valid Item> additionalItems) {
        public Map<Long, String> replyMap() {
            var result = new LinkedHashMap<Long, String>();
            for (var r : replies)
                if (result.putIfAbsent(r.commentId(), r.content()) != null)
                    throw new com.eldercare.backend.common.error.BusinessException(
                            org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY,
                            "INVALID_VALUE",
                            "Không lặp phản hồi cho một ý kiến");
            return result;
        }
    }
}
