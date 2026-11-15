package com.eldercare.backend.roombedallocation;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;

/**
 * 019 T016 lượt 1 (US4 kịch bản 1 → 3, 7, 8; FR-008c, FR-011 → FR-013, FR-021; BR-M03-18, DBR-36, Q-284, Q-292, Q-294,
 * Q-313; quickstart Q18): ghi tăng, đưa vào sử dụng, chuyển vị trí, báo hỏng theo tầng, bảo trì và kết quả, chuyển bộ
 * phận, danh mục loại có hai loại hệ thống Giường, Xe.
 */
class FR_013_AssetLifecycleIT extends AssetTestBase {

    /**
     * FR-012 (api-019 chủ dự án duyệt 2026-11-10): chi tiết tài sản có phiên bản cho If-Match, lệnh người xem làm được
     * (chỉ để hiện nút) và lịch sử vị trí, bảo trì.
     */
    @Test
    void FR_012_detailCarriesVersionAllowedCommandsAndHistory() throws Exception {
        long id = created(donated("Hội người cao tuổi phường tặng"));
        command(id, "put-into-use", "{\"location\":{\"type\":\"FLOOR\",\"id\":" + floor + "}}")
                .andExpect(status().isOk());
        command(id, "start-maintenance", "{}").andExpect(status().isOk());
        command(
                        id,
                        "record-maintenance-result",
                        "{\"result\":\"PASS\",\"date\":\"" + time.today()
                                + "\",\"content\":\"Thay bánh xe\",\"performedBy\":\"Tổ kỹ thuật\"}")
                .andExpect(status().isOk());

        mvc.perform(get(URL + "/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("READY"))
                // Trường mô tả để màn Sửa mô tả điền sẵn (chủ dự án duyệt 2026-11-12)
                .andExpect(jsonPath("$.source").value("DONATED"))
                .andExpect(jsonPath("$.sourceDescription").value("Hội người cao tuổi phường tặng"))
                .andExpect(jsonPath("$.version").isNumber())
                .andExpect(
                        jsonPath("$.locations[*].type").value(org.hamcrest.Matchers.contains("ASSET_STORAGE", "FLOOR")))
                .andExpect(jsonPath("$.maintenances[0].result").value("PASS"))
                .andExpect(jsonPath("$.maintenances[0].content").value("Thay bánh xe"))
                .andExpect(jsonPath("$.maintenances[0].voided").value(false))
                .andExpect(jsonPath("$.allowedCommands")
                        .value(org.hamcrest.Matchers.hasItems(
                                "update", "relocate", "report-damage", "start-maintenance", "request-disposal")))
                .andExpect(jsonPath("$.allowedCommands")
                        .value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasItem("transfer-department"))));
        as(manager);
        mvc.perform(get(URL + "/" + id))
                .andExpect(jsonPath("$.allowedCommands").value(org.hamcrest.Matchers.hasItem("transfer-department")));
    }
}
