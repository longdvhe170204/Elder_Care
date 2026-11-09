package com.eldercare.backend.billing.api.spi;

/**
 * 019 FR-008d (Q-337; chủ dự án duyệt 2026-11-06 đảo chiều thay cho M11 đọc {@code medication.api.PrescriptionApi}, vì
 * M11 → M07 → M04 → M11 là vòng): M11 khai, M07 (006) cài. Thiếu bean thì lệnh lập, sửa yêu cầu loại thuốc trả 503
 * {@code SOURCE_UNAVAILABLE} (mặc định an toàn).
 */
public interface ProcurementPrescriptionPort {
    /** Đơn {@code prescriptionId} là đơn nguồn viện Hiệu lực, Tạm dừng hoặc Chờ hiệu lực của {@code residentId}. */
    boolean isUnendedFacilityPrescription(long residentId, long prescriptionId);
}
