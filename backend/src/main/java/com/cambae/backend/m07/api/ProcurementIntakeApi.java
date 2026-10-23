package com.cambae.backend.m07.api;

import com.cambae.backend.m07.api.dto.IntakeResultView;
import java.util.List;

/**
 * API kết quả tiếp nhận thuốc cung ứng (TiepNhanThuocCungUngApi, FR-054, Q-337).
 * Dùng bởi Module M11/M03 (019 - Mua sắm & Tài sản).
 */
public interface ProcurementIntakeApi {
    /**
     * Danh sách các lần tiếp nhận và kết quả đạt/không đạt theo từng yêu cầu mua sắm.
     */
    List<IntakeResultView> byProcurementRequest(long procurementRequestId);
}
