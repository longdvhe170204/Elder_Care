package com.eldercare.backend.family.web.response;

import com.eldercare.backend.administration.shared.api.dto.SimpleRecord;
import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.notification.FamilyRight;
import com.eldercare.backend.family.internal.right.domain.RightChangeRequest;
import com.eldercare.backend.family.internal.right.domain.RightChangeRequest.ConfirmationMethod;
import com.eldercare.backend.family.internal.right.domain.RightChangeRequest.Source;
import com.eldercare.backend.family.internal.right.domain.RightChangeRequest.Status;
import com.eldercare.backend.family.internal.right.domain.RightChangeRequest.Type;
import com.eldercare.backend.family.web.request.CreateRightChangeRequestBody;
import com.eldercare.backend.family.web.request.CreateRightChangeRequestBody.NewPickerBody;
import io.swagger.v3.oas.annotations.media.Schema;

// Hợp đồng ghi allOf(CreateRightChangeRequest, {trạng thái}); JSON vẫn là một object phẳng
@Schema(
        name = "RightChangeRequest",
        allOf = {CreateRightChangeRequestBody.class, RightChangeRequestResponse.State.class})
public record RightChangeRequestResponse(
        long residentId,
        Type type,
        Long relationshipId,
        FamilyRight right,
        Boolean newValue,
        NewPickerBody newPicker,
        Boolean withSignedForm,
        Long signedByFamilyMemberId,
        Long evidenceFileId,
        Reason reason,
        long id,
        Status status,
        ConfirmationMethod confirmationMethod,
        Source source,
        boolean appliedOnCreation,
        Long conflictingWishId) {

    /** Phần trạng thái của yêu cầu (data-model mục 3). */
    @Schema(name = "RightChangeRequestState")
    public record State(
            long id,
            Status status,
            ConfirmationMethod confirmationMethod,
            Source source,
            boolean appliedOnCreation,
            Long conflictingWishId) {}

    public static RightChangeRequestResponse of(SimpleRecord<RightChangeRequest> record) {
        RightChangeRequest r = record.content();
        boolean signed = r.confirmationMethod() == ConfirmationMethod.SIGNED_FORM;
        RightChangeRequest.NewPicker p = r.newPicker();
        return new RightChangeRequestResponse(
                record.residentId(),
                r.type(),
                r.relationshipId(),
                r.right(),
                r.newValue(),
                p == null
                        ? null
                        : new NewPickerBody(
                                p.fullName(),
                                p.phoneNumber(),
                                p.idDocumentType(),
                                p.idDocumentNumber(),
                                p.relationship()),
                signed,
                signed ? r.confirmedByFamilyMemberId() : null,
                r.evidenceFileId(),
                r.reason() == null ? null : new Reason(r.reason()),
                record.id(),
                Status.valueOf(record.status()),
                r.confirmationMethod(),
                r.source(),
                r.appliedOnCreation(),
                r.conflictingWishId());
    }
}
