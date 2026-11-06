package com.eldercare.backend.family.internal.portal.domain;

import java.util.Arrays;
import java.util.Optional;

/**
 * Phần sức khỏe của cổng (api-012 {@code section}) và mã phần của nhật ký lượt xem ({@code nhat_ky_xem_suc_khoe.phan};
 * data-model mục 7; R-15: hành động nhật ký). Mã {@code THONG_BAO} của nhật ký thuộc lượt mở thông báo, không phải phần
 * của cổng.
 */
public enum HealthSection {
    ALLERGIES_CONDITIONS("allergies-conditions"),
    VITAL_SIGNS("vital-signs"),
    WEIGHT("weight"),
    MEDICATIONS("medications"),
    INCIDENTS("incidents"),
    CARE_RESULTS("care-results"),
    EATING("eating"),
    DIET("diet"),
    MEDICAL_APPOINTMENTS("medical-appointments");

    private final String path;

    HealthSection(String path) {
        this.path = path;
    }

    public String path() {
        return path;
    }

    public static Optional<HealthSection> fromPath(String path) {
        return Arrays.stream(values()).filter(s -> s.path.equals(path)).findFirst();
    }
}
