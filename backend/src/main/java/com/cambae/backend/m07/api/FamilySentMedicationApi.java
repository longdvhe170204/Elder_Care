package com.cambae.backend.m07.api;

import com.cambae.backend.m07.api.dto.FamilyMedicationView;
import java.util.List;

/**
 * API tra cứu thuốc gia đình gửi (ThuocGiaDinhGuiApi).
 * Dùng bởi Module M02 (004 - Viện phí & Kết thúc lưu trú), M10 (012), M14 (016).
 */
public interface FamilySentMedicationApi {
    /** Kiểm tra xem viện còn đang giữ thuốc gia đình gửi của người cao tuổi không (004 FR-063c). */
    boolean hasRetainedMedication(long residentId);

    /** Danh sách thuốc gia đình gửi của người cao tuổi (tên, trạng thái, số lượng còn, hạn dùng). */
    List<FamilyMedicationView> byResident(long residentId);
}
