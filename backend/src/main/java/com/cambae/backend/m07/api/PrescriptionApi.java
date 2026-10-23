package com.cambae.backend.m07.api;

import com.cambae.backend.m07.api.dto.ActiveMedicationView;
import com.cambae.backend.m07.api.dto.PrescriptionView;
import java.util.List;

/**
 * API đọc thông tin đơn thuốc (DonThuocApi).
 * Dùng bởi Module M05/M06 (007 - Thẻ khẩn cấp, chuyển viện) và M14 (016 - Báo cáo).
 */
public interface PrescriptionApi {
    /** Danh sách đơn nguồn viện chưa kết thúc (Hiệu lực, Tạm dừng, Chờ hiệu lực). */
    List<PrescriptionView> unendedFacilityPrescriptions(long residentId);

    /** Thuốc đang dùng cho thẻ khẩn cấp, tóm tắt chuyển viện (FR-050). */
    List<ActiveMedicationView> activeMedications(long residentId);

    /** Chuỗi lịch sử thay thế đơn thuốc theo thời gian (FR-016a). */
    List<PrescriptionView> history(long residentId);
}
