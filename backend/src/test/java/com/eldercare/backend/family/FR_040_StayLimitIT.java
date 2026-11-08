package com.eldercare.backend.family;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

/**
 * 012 T034 ({@code FR_040_GioiHanOLaiIT}; quickstart Q30; US8 kịch bản 4, 6; FR-040, FR-041, FR-045; Phụ lục 27 chú thích
 * 20): mỗi người cao tuổi tối đa CFG-M10-10 (mặc định 1) lượt Đã xác nhận hoặc Đang ở lại chồng thời gian; một người thân
 * không có hai lượt mở chồng; khoanh vùng, không Đang lưu trú thì chặn đăng ký và bắt đầu; ai làm lệnh nào theo bảng
 * FR-045, Quản lý viện xác nhận thay khi tầng không có Trưởng tầng được giao.
 */
class FR_040_StayLimitIT extends StayTestBase {

    @Test
    void Q30_overlapWithConfirmedStayOfSameResident_isRejected() throws Exception {
        confirmed(rMember, at(day, 18, 0), at(day.plusDays(3), 8, 0));

        register(office, mMember, at(day.plusDays(1), 18, 0), at(day.plusDays(2), 8, 0))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("FAMILY_STAY_OVERLAP"));
        register(office, mMember, at(day.plusDays(3), 9, 0), at(day.plusDays(4), 8, 0))
                .andExpect(status().isCreated());
    }
}
