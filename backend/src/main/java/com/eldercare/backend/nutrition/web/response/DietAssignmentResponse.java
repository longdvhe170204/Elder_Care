package com.eldercare.backend.nutrition.web.response;

import com.eldercare.backend.nutrition.api.dto.DietAssignmentStatus;
import com.eldercare.backend.nutrition.api.dto.Texture;
import com.eldercare.backend.nutrition.internal.assignment.domain.AssignmentBasis;
import com.eldercare.backend.nutrition.internal.assignment.domain.DietAssignment;
import com.eldercare.backend.nutrition.internal.assignment.service.DietAssignmentService.AssignmentRow;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

/**
 * Bản gán (api-011 {@code DietAssignment}). Dòng {@code isDefault = true} là chế độ ăn mặc định của viện đang dùng vì
 * chưa có bản Hiệu lực (FR-014): không có id, trạng thái; kết cấu thường, không hạn chế riêng.
 */
@Schema(name = "DietAssignment")
public record DietAssignmentResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long dietId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Texture texture,
        List<Long> personalRestrictions,
        String personalInstruction,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) AssignmentBasis basis,
        Long basisId,
        Long reviewNoticeId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Instant effectiveFrom,
        Long id,
        DietAssignmentStatus status,
        Instant actualEffectiveAt,
        String professionalReason,
        Boolean isDefault) {

    public static DietAssignmentResponse of(AssignmentRow row) {
        if (row.assignment().isEmpty()) {
            return new DietAssignmentResponse(
                    row.dietId(),
                    Texture.REGULAR,
                    List.of(),
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    true);
        }
        DietAssignment a = row.assignment().get();
        return new DietAssignmentResponse(
                a.dietId(),
                a.texture(),
                a.personalRestrictions(),
                a.personalInstruction(),
                a.basis(),
                a.basisId(),
                a.reviewNoticeId(),
                a.requestedEffectiveFrom(),
                a.id(),
                a.status(),
                a.actualEffectiveAt(),
                a.professionalReason(),
                false);
    }
}
