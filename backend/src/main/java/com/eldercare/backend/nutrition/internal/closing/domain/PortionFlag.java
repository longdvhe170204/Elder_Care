package com.eldercare.backend.nutrition.internal.closing.domain;

/**
 * Dấu của suất một người (data-model mục 7 {@code suat_dac_biet.dau}; glossary mục 17.3): dự kiến trở về (FR-034 a, FR-040a),
 * cần đối chiếu khi phục vụ (FR-036a), thiếu món thay thế (FR-036), món an toàn (FR-037a), chưa có món trong thực đơn
 * (FR-038), chưa gán chế độ ăn (FR-014), suất giữ (FR-040a).
 */
public enum PortionFlag {
    EXPECTED_RETURN,
    NEEDS_CROSS_CHECK,
    MISSING_SUBSTITUTE_DISH,
    SAFE_DISH,
    NO_DISH_IN_MENU,
    NO_DIET_ASSIGNED,
    HELD_PORTION
}
