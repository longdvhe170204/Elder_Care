package com.eldercare.backend.admission.api.dto;

/** Loại dịch vụ (FR-034). */
public enum ServiceType {
    CARE,
    RESIDENCY_FEE,
    DAY_CARE_SESSION,
    REHABILITATION,
    ACTIVITY,
    MEDICAL_ESCORT,
    INJECTION,
    FAMILY_STAY,
    MEAL,
    FACILITY_SUPPLIED_MEDICATION,
    /** Vật phẩm tiêu hao tính phí (tã, bỉm, sữa; nghiệp vụ 6.4). */
    CONSUMABLE,
    /** Vật tư y tế (Q-269: được mua gấp vượt số đã đồng ý). */
    MEDICAL_SUPPLY,
    OTHER;
}
