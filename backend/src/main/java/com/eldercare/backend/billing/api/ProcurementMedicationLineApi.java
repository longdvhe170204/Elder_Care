package com.eldercare.backend.billing.api;

import com.eldercare.backend.billing.api.dto.ProcurementMedicationLineView;
import java.util.Optional;

/**
 * 019 FR-008d cho 006 FR-054, BR-M07-24 (chủ dự án duyệt 2026-11-08 thay {@code medication.api.spi.ProcurementMedicationPort}):
 * M11 cài, M07 đọc khi ghi lần tiếp nhận thuốc cung ứng theo dòng. Chỉ đọc.
 */
public interface ProcurementMedicationLineApi {
    /**
     * Dòng {@code lineId} (id dòng của schema {@code ProcurementRequestLine}, api-019) của yêu cầu loại thuốc đã được
     * Phụ trách bộ phận ghi đã mua (Chờ đối chiếu, Lệch, Đã đối chiếu); khác thì rỗng.
     */
    Optional<ProcurementMedicationLineView> lineRecordedPurchased(long lineId);
}
