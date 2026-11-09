package com.eldercare.backend.belongings.web.response;

import com.eldercare.backend.belongings.internal.returns.service.RecipientConfirmationService;
import java.time.Instant;
import java.util.List;

/** Schema {@code RecipientConfirmation} của api-013; {@code belongingIds} là đồ còn trong danh sách (FR-017b). */
public record RecipientConfirmationResponse(
        long id,
        long residentId,
        String recipientName,
        String recipientIdDocument,
        boolean recipientIsResident,
        List<Long> belongingIds,
        String status,
        String method,
        Instant expiresAt) {

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
                view.expiresAt());
    }
}
