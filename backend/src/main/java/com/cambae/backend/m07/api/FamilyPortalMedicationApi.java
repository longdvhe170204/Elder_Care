package com.cambae.backend.m07.api;

import com.cambae.backend.m07.api.dto.ConfirmedReconciliationView;
import com.cambae.backend.m07.api.dto.FamilyMedicationView;
import com.cambae.backend.m07.api.dto.FamilyPrescriptionView;
import java.util.List;
import java.util.Optional;

/**
 * API dành riêng cho Cổng người thân (ThuocCongNguoiThanApi, FR-049, Q-59).
 * Dùng bởi Module M10 (012 - Family Portal).
 */
public interface FamilyPortalMedicationApi {
    /** Danh sách thuốc người thân đã gửi viện (cho người thân có quan hệ Hiệu lực). */
    List<FamilyMedicationView> sentMedications(long residentId, long familyMemberId);

    /** Đơn thuốc của người cao tuổi (chỉ trả về khi có bản đồng ý bao gồm người thân đó). */
    Optional<List<FamilyPrescriptionView>> prescriptions(long residentId, long familyMemberId);

    /** Phiếu đối chiếu thuốc đã xác nhận (chỉ trả về khi có bản đồng ý bao gồm người thân đó). */
    Optional<List<ConfirmedReconciliationView>> confirmedReconciliations(long residentId, long familyMemberId);
}
