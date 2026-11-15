package com.eldercare.backend.admission;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;

/**
 * 004 T057 (US7; FR-062, FR-066, FR-067; contracts/api-004.yaml, tag {@code residency-ending}): lập, xem, đổi ngày, hủy và
 * ghi nhận bàn giao của hồ sơ kết thúc lưu trú. Mỗi người tối đa một hồ sơ Đang chuẩn bị; Chuyển cơ sở bắt buộc nơi chuyển
 * đến; Bác sĩ chỉ lập Xuất viện, Chuyển cơ sở vì lý do y tế.
 */
@AutoConfigureMockMvc
@WithMockUser
class ResidencyEndingIT extends EndingTestBase {

    /** "Đổi api" 004 (A duyệt 2026-11-07): danh sách hồ sơ kết thúc lọc theo người cao tuổi và trạng thái. */
    @Test
    void FR_062_listEndings_filtersByResidentAndStatus() throws Exception {
        long residentId = inResidence();
        long id = createEnding(residentId, d0.plusDays(10));

        user.set(office.user());
        mvc.perform(get("/api/v1/residency-endings")
                        .param("residentId", String.valueOf(residentId))
                        .param("status", "PREPARING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(id))
                .andExpect(jsonPath("$[0].status").value("PREPARING"));
        mvc.perform(get("/api/v1/residency-endings")
                        .param("residentId", String.valueOf(residentId))
                        .param("status", "CANCELLED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
