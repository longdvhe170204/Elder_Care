package com.eldercare.backend.nutrition.internal.closing.domain;

/**
 * Loại người được tính suất (data-model mục 6, 13.4; FR-034 a, b, c, d): nội trú, bán trú, người thân ở lại, Quản lý viện
 * và nhân viên trong ca ({@code STAFF}, mã người là mã hồ sơ nhân viên, tầng/khu {@link PortionContent#STAFF_GROUP}).
 */
public enum CountedKind {
    RESIDENTIAL,
    DAY_CARE,
    FAMILY,
    STAFF
}
