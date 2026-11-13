package com.eldercare.backend.billing;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.eldercare.backend.support.TestUsers;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/**
 * 019 T005 (US7 kịch bản 1, 2, 5, 7; FR-008, FR-020, FR-021; BR-M11-17; Q-290, Q-292, Q-293, Q-299; quickstart Q1, Q2,
 * Q9): vòng đời Nháp → Chờ duyệt → Đã duyệt/Từ chối, chỉ Phụ trách bộ phận của bộ phận lập, nội dung khóa khi rời Nháp.
 */
class BR_M11_17_ProcurementRequestIT extends ProcurementTestBase {

    /**
     * api-019 (chủ dự án duyệt 2026-11-10): danh sách trả phiên bản cho If-Match, người lập và allowedCommands theo người
     * xem (chỉ để hiện nút, constitution XI).
     */
    @Test
    void FR_008_listCarriesVersionCreatorAndAllowedCommandsOfTheViewer() throws Exception {
        long id = created(ingredientBody(kitchen));
        String row = "$[?(@.id==" + id + ")]";
        var list = get(URL).param("departmentId", Long.toString(kitchen));
        mvc.perform(list)
                .andExpect(status().isOk())
                .andExpect(jsonPath(row + ".version").isNotEmpty())
                .andExpect(jsonPath(row + ".createdByStaffId")
                        .value(org.hamcrest.Matchers.contains((int) kitchenLead.staffId())))
                .andExpect(jsonPath(row + ".allowedCommands[*]")
                        .value(org.hamcrest.Matchers.containsInAnyOrder("update", "submit", "cancel")));
        as(manager);
        mvc.perform(list).andExpect(jsonPath(row + ".allowedCommands[*]").isEmpty());

        as(kitchenLead);
        command(id, "submit").andExpect(status().isOk());
        mvc.perform(list)
                .andExpect(jsonPath(row + ".allowedCommands[*]").value(org.hamcrest.Matchers.contains("withdraw")));
        as(manager);
        mvc.perform(list)
                .andExpect(jsonPath(row + ".allowedCommands[*]")
                        .value(org.hamcrest.Matchers.containsInAnyOrder("approve", "reject")));
    }

    private org.springframework.test.web.servlet.ResultActions update(long id, long version) throws Exception {
        return mvc.perform(put(URL + "/" + id)
                .header("Idempotency-Key", "pu-" + TestUsers.nextId())
                .header("If-Match", "\"" + version + "\"")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(kitchen, "INGREDIENT", "Hết gạo, thêm thịt")));
    }
}
