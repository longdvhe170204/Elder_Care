package com.cambae.backend.m15.account.api;

import java.util.Optional;

/**
 * Đọc danh mục nghiệp vụ chuyên môn (002 contracts mục 2: {@code NghiepVuChuyenMonApi.loaiYeuCau}; 008 FR-030): 008
 * biết loại giấy phép/đào tạo của nghiệp vụ "phát thuốc và xác nhận liều" để chặn phân công Điều dưỡng phụ trách.
 */
public interface ProfessionalActivityApi {

    /** Mã nghiệp vụ mặc định "phát thuốc và xác nhận liều". */
    String MEDICATION_ADMINISTRATION = "MEDICATION_ADMINISTRATION";

    /** Mã nghiệp vụ mặc định "chẩn đoán và kê đơn nội bộ". */
    String INTERNAL_DIAGNOSIS_PRESCRIBING = "INTERNAL_DIAGNOSIS_PRESCRIBING";

    /** Loại giấy phép/đào tạo yêu cầu (mã loại của 008) của mục đang hiệu lực; rỗng khi chưa cấu hình hoặc không có. */
    Optional<String> requiredLicenseType(String activityCode);
}
