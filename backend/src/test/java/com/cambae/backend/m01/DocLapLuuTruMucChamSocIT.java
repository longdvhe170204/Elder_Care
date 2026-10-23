package com.cambae.backend.m01;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * 001 T061 (FR-052): mức chăm sóc và loại hình lưu trú có lịch sử độc lập. M01 không lưu loại hình lưu trú hiện hành
 * hay lịch sử của nó trên hồ sơ (lấy từ hợp đồng qua 004), nên bảng hồ sơ và bảng lịch sử mức không có cột loại hình
 * lưu trú.
 */
class DocLapLuuTruMucChamSocIT extends StatusTestBase {

    @Test
    void FR_052_residentAndCareLevelHistoryHoldNoResidenceType() {
        Integer columns = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE()"
                        + " AND table_name IN ('resident', 'care_level_history')"
                        + " AND column_name LIKE '%residence_type%'",
                Integer.class);

        assertThat(columns).isZero();
    }
}
