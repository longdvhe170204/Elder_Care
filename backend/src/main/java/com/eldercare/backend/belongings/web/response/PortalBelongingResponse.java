package com.eldercare.backend.belongings.web.response;

import com.eldercare.backend.belongings.api.dto.BelongingStatus;
import com.eldercare.backend.belongings.internal.belonging.domain.HandoverView;
import com.eldercare.backend.belongings.internal.belonging.service.BelongingPortalQueries;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Đồ gửi trên cổng người thân (api-013 {@code GET /family-portal/residents/{residentId}/belongings}, K-12): không có vị
 * trí lưu giữ, người giữ chỉ là loại ({@code STAFF}, {@code RESIDENT}), lịch sử không có tên nhân viên.
 */
public record PortalBelongingResponse(
        long id,
        long typeId,
        String description,
        BigDecimal quantity,
        @Schema(type = "integer", format = "int64") BigDecimal cashAmount,
        BelongingStatus status,
        String holder,
        List<Entry> history,
        SelfCustodyConsentResponse selfCustodyConsent) {

    /** Một dòng lịch sử: loại bản ghi, trạng thái đích, thời điểm, số lượng, tình trạng, ảnh. */
    @Schema(name = "PortalBelongingHistoryEntry")
    public record Entry(
            long id,
            String kind,
            String targetStatus,
            Instant occurredAt,
            BigDecimal quantity,
            @Schema(type = "integer", format = "int64") BigDecimal cashAmount,
            String conditionDescription,
            List<Long> photoFileIds) {

        static Entry of(HandoverView h) {
            return new Entry(
                    h.id(),
                    h.kind(),
                    h.targetStatus(),
                    h.occurredAt(),
                    h.quantity(),
                    h.cashAmount(),
                    h.conditionDescription(),
                    h.photoFileIds());
        }
    }

    public static PortalBelongingResponse of(BelongingPortalQueries.Item item) {
        var b = item.belonging();
        return new PortalBelongingResponse(
                b.belongingId(),
                b.typeId(),
                b.description(),
                b.quantity(),
                b.cashAmount(),
                b.status(),
                b.holderType() == null ? null : b.holderType().name(),
                item.history().stream().map(Entry::of).toList(),
                SelfCustodyConsentResponse.of(item.selfCustodyConsent()));
    }
}
