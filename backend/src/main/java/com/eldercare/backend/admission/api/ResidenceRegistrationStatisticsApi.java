package com.eldercare.backend.admission.api;

import com.eldercare.backend.admission.api.dto.RegistrationSummaryView;

/**
 * Số liệu khai báo tạm trú cho báo cáo 016 (tên logic {@code KhaiBaoThongKeApi}; contracts/module-interfaces.md mục 2); chỉ
 * đọc, không khóa. Quyền xem toàn viện do 016 áp (Quản lý viện, Hành chính).
 */
public interface ResidenceRegistrationStatisticsApi {

    /** Bốn số của dashboard (016 FR-020). */
    RegistrationSummaryView summary();
}
