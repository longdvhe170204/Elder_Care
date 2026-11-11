package com.eldercare.backend.belongings.web.response;

import com.eldercare.backend.administration.shared.api.dto.SimpleRecord;
import com.eldercare.backend.belongings.api.dto.BelongingStatus;
import com.eldercare.backend.belongings.api.dto.BelongingView;
import com.eldercare.backend.belongings.internal.belonging.domain.BelongingDetail;
import com.eldercare.backend.belongings.internal.belonging.domain.HandoverView;
import com.eldercare.backend.belongings.internal.consent.domain.SelfCustodyConsent;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Schema {@code Belonging} của api-013; {@code holder} là loại người giữ ({@code STAFF}, {@code RESIDENT}). */
public record BelongingResponse(
        long id,
        long residentId,
        long typeId,
        String description,
        BigDecimal quantity,

        @Schema(
                types = {"integer"},
                format = "int64")
        BigDecimal cashAmount,

        BelongingStatus status,
        String holder,
        Long storageLocationId,
        Long originalBelongingId,
        Instant lastHandoverAt,
        int version) {

    public static BelongingResponse of(BelongingView b) {
        return new BelongingResponse(
                b.belongingId(),
                b.residentId(),
                b.typeId(),
                b.description(),
                b.quantity(),
                b.cashAmount(),
                b.status(),
                b.holderType() == null ? null : b.holderType().name(),
                b.storageLocationId(),
                b.originalBelongingId(),
                b.lastHandoverAt(),
                b.version());
    }

    /** Chi tiết đồ gửi và lịch sử bàn giao ({@code GET /belongings/{id}}; hợp đồng không khai schema). */
    public record Detail(
            BelongingResponse belonging, List<HandoverView> handovers, SelfCustodyConsentResponse selfCustodyConsent) {

        public static Detail of(BelongingDetail detail, SimpleRecord<SelfCustodyConsent> consent) {
            return new Detail(
                    BelongingResponse.of(detail.belonging()),
                    detail.handovers(),
                    SelfCustodyConsentResponse.of(consent));
        }
    }
}
