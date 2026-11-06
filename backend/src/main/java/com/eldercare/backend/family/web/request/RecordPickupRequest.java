package com.eldercare.backend.family.web.request;

import com.eldercare.backend.family.internal.pickup.domain.PickupType;
import com.eldercare.backend.family.internal.pickup.service.PickupService.RecordCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Thân lệnh ghi đón ({@code RecordPickup}; FR-021). */
@Schema(name = "RecordPickup")
public record RecordPickupRequest(
        @NotNull PickupType type,
        Long familyMemberId,
        Long exceptionId,
        @Valid PresentedIdDocument presentedIdDocument,
        @NotNull Long handoverStaffId) {

    /** Giấy tờ người đón xuất trình (FR-021 bước 3). */
    public record PresentedIdDocument(
            @Size(max = 40) String type, @Size(max = 40) String number) {}

    public RecordCommand command() {
        String number = presentedIdDocument == null || presentedIdDocument.number() == null
                ? null
                : presentedIdDocument.number().strip();
        return new RecordCommand(
                type,
                familyMemberId,
                exceptionId,
                presentedIdDocument == null ? null : presentedIdDocument.type(),
                number,
                handoverStaffId);
    }
}
