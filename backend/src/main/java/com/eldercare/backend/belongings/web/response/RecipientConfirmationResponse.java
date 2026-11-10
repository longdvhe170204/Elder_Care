package com.eldercare.backend.belongings.web.response;

import com.eldercare.backend.belongings.internal.returns.service.RecipientConfirmationService;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

/** Schema {@code RecipientConfirmation} của api-013; {@code belongingIds} là đồ còn trong danh sách (FR-017b). */
@Schema(requiredProperties = {"residentId", "recipientName", "recipientIdDocument", "belongingIds"})
public record RecipientConfirmationResponse(
        long id,
        long residentId,
        String recipientName,
        String recipientIdDocument,
        boolean recipientIsResident,
        List<Long> belongingIds,
        String status,
        String method,
        Instant expiresAt,
        SignedForm signedForm) {

    /** Phiếu người đại diện ký (cách lập "phiếu ký", FR-017a); trống khi lập cách khác. */
    @Schema(name = "RecipientSignedForm")
    public record SignedForm(Long representativeId, Long fileId) {}

    public static RecipientConfirmationResponse of(RecipientConfirmationService.View view) {
        return new RecipientConfirmationResponse(
                view.id(),
                view.residentId(),
                view.recipientName(),
                view.recipientIdDocument(),
                view.recipientIsResident(),
                view.belongingIds(),
                view.status(),
                view.method(),
                view.expiresAt(),
                view.signedFormFileId() == null
                        ? null
                        : new SignedForm(view.signedRepresentativeId(), view.signedFormFileId()));
    }
}
