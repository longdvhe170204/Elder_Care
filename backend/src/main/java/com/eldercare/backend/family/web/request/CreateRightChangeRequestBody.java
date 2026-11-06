package com.eldercare.backend.family.web.request;

import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.notification.FamilyRight;
import com.eldercare.backend.family.internal.right.domain.RightChangeRequest.NewPicker;
import com.eldercare.backend.family.internal.right.domain.RightChangeRequest.Type;
import com.eldercare.backend.family.internal.right.service.RightChangeRequestService.CreateCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * Thân lập yêu cầu thay đổi quyền (api-012 {@code CreateRightChangeRequest}; FR-013). {@code signedByFamilyMemberId} là
 * người đại diện ký khi Hành chính ghi kèm bản ký ({@code withSignedForm}, FR-015 b).
 */
@Schema(name = "CreateRightChangeRequest")
public record CreateRightChangeRequestBody(
        @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        Long residentId,

        @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        Type type,

        Long relationshipId,
        FamilyRight right,
        Boolean newValue,
        NewPickerBody newPicker,
        Boolean withSignedForm,
        Long signedByFamilyMemberId,
        Long evidenceFileId,

        @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        Reason reason) {

    /** Người đón mới chưa có hồ sơ (FR-013); số điện thoại, loại giấy tờ cho hồ sơ (FR-001, FR-007). */
    @Schema(name = "NewPicker")
    public record NewPickerBody(
            String fullName, String phoneNumber, String idDocumentType, String idDocumentNumber, String relationship) {

        NewPicker picker() {
            return new NewPicker(fullName, phoneNumber, idDocumentType, idDocumentNumber, relationship);
        }
    }

    public CreateCommand command() {
        return new CreateCommand(
                residentId,
                type,
                relationshipId,
                right,
                newValue,
                newPicker == null ? null : newPicker.picker(),
                Boolean.TRUE.equals(withSignedForm),
                signedByFamilyMemberId,
                evidenceFileId);
    }
}
