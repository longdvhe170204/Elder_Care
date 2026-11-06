package com.eldercare.backend.billing.internal.procurement.domain;

import com.eldercare.backend.billing.api.dto.ProcurementStatus;
import com.eldercare.backend.roombedallocation.api.dto.ProcurementSourceType;
import com.eldercare.backend.roombedallocation.api.dto.ProcurementType;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** Một dòng {@code procurement_request} (019 data-model mục 3, 0.1); tiền theo đồng. */
public record ProcurementRequestRow(
        long id,
        long departmentId,
        long createdByStaffId,
        ProcurementType type,
        String priority,
        Instant neededBy,
        List<Long> relatedFileIds,
        List<Line> lines,
        long estimatedTotal,
        String reason,
        LocalDate neededOn,
        Map<String, Object> reference,
        Long originalRequestId,
        Long approvedAmount,
        String reductionReason,
        Long decidedByStaffId,
        Instant approvedAt,
        Long actualAmount,
        LocalDate purchasedOn,
        String supplier,
        List<Long> receiptFileIds,
        Long purchaseRecordedByStaffId,
        Instant purchaseRecordedAt,
        String reconciliationResult,
        Long discrepancyAmount,
        String discrepancyResolution,
        Long reconciledByStaffId,
        List<LinkedSource> linkedSources,
        ProcurementStatus status,
        String statusReason,
        Instant createdAt,
        Instant lockedAt,
        int version) {

    /** Dòng của yêu cầu (yeu_cau_mua_sam_dong); trường thuốc chỉ có ở loại MEDICATION (FR-008d). */
    public record Line(
            String name,
            BigDecimal quantity,
            long estimatedUnitPrice,
            long amount,
            Long residentId,
            Long prescriptionId,
            String activeIngredient,
            String strength,
            String dosageForm,
            String unit) {}

    /** Nguồn gắn (mua_sam_gan_nguon; DBR-36): tài sản mua mới hoặc lần bảo trì thuê ngoài. */
    public record LinkedSource(ProcurementSourceType sourceType, long sourceId, long amount, Instant linkedAt) {}

    public long linkedSourceTotal() {
        return linkedSources.stream().mapToLong(LinkedSource::amount).sum();
    }
}
