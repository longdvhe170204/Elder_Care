package com.eldercare.backend.incident.api;

/**
 * Tra id loại sự cố theo mã danh mục (bổ sung 2026-11-07, 011 Phase 8): bên gọi {@link IncidentCommandApi#record} chỉ biết
 * mã loại (ví dụ {@code WRONG_MEAL_SERVED}), còn kind {@code M05.INCIDENT_TYPE} thuộc nội bộ 007.
 */
public interface IncidentTypeApi {

    /** 404 khi chưa có loại sự cố mang mã này. */
    long idOf(String code);
}
