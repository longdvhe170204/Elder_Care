package com.eldercare.backend.roombedallocation.internal.bed.domain;

/** Tách từ {@code BedAssetStateStore.FutureAllocation} (LayerArchTest: record ở {@code domain}). */
public record FutureAllocation(long id, long residentId, String basisType, Long basisId, Long previousId) {
    public boolean hasLinkedSource() {
        return basisType != null || basisId != null || previousId != null;
    }

    /** Phân bổ đặt trước cho yêu cầu chuyển theo lịch (căn cứ là yêu cầu phê duyệt, T033). */
    public boolean scheduledTransfer() {
        return "APPROVAL_REQUEST".equals(basisType) && basisId != null;
    }
}
