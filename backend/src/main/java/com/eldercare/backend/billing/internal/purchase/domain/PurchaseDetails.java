package com.eldercare.backend.billing.internal.purchase.domain;

import com.eldercare.backend.administration.shared.api.dto.SharedKind;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/** Nội dung M11 của hồ sơ chung; trạng thái và khóa do SimpleRecordApi bảo vệ. */
public record PurchaseDetails(
        Long catalogItemId,
        String description,
        BigDecimal quantity,
        String reason,
        String requesterType,
        long requesterId,
        PurchaseConsent consent,
        int round,
        Instant sentAt,
        boolean insufficientBalance,
        Long actualAmount,
        LocalDate purchasedOn,
        Long receiptFileId,
        Long purchaserStaffId) {
    public static final SharedKind<PurchaseDetails> KIND =
            SharedKind.of("M11.PURCHASE_ASSISTANCE_REQUEST", PurchaseDetails.class);

    public PurchaseDetails withConsent(PurchaseConsent next, int nextRound, Instant sent, boolean insufficient) {
        return new PurchaseDetails(
                catalogItemId,
                description,
                quantity,
                reason,
                requesterType,
                requesterId,
                next,
                nextRound,
                sent,
                insufficient,
                actualAmount,
                purchasedOn,
                receiptFileId,
                purchaserStaffId);
    }

    public PurchaseDetails purchased(long amount, LocalDate date, long file, long staff) {
        return new PurchaseDetails(
                catalogItemId,
                description,
                quantity,
                reason,
                requesterType,
                requesterId,
                consent.purchase(),
                round,
                sentAt,
                insufficientBalance,
                amount,
                date,
                file,
                staff);
    }
}
