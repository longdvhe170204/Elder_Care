package com.eldercare.backend.incident;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/**
 * BR-M05-29, US13 #1, #2, #4 → #6, SC-019 (T097; quickstart H1, H2, H4 → H6): trong ca mới bấm được, ngoài ca 409
 * {@code NOT_ON_SHIFT} kèm số liên hệ; gộp theo người bấm; chi tiết đặt một lần, người cao tuổi gắn vào cảnh báo; chuyển
 * sự cố, kích hoạt khẩn cấp thiếu người cao tuổi → 422 {@code RESIDENT_REQUIRED}; quyền theo FR-080.
 */
class BR_47_EmergencyAssistButtonIT extends AssistTestBase {

    @Test
    void H1_pressCreatesCriticalAssistAlertWithoutResident() throws Exception {
        pressAssist(presser)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SENT"));

        var alert = jdbc.queryForMap(
                "SELECT resident_id, floor_area_id, level, type, occurrence_count FROM alert WHERE id = ?",
                assistAlert(presser));
        assertThat(alert.get("resident_id")).isNull();
        assertThat(((Number) alert.get("floor_area_id")).longValue()).isEqualTo(floor);
        assertThat(alert.get("level")).isEqualTo("CRITICAL");
        // 007 T106 (chủ dự án chốt 2026-11-10): lần bấm gắn vào cảnh báo, xóa cùng cảnh báo theo hạn lưu giữ
        assertThat(jdbc.queryForList(
                        "SELECT entity_type FROM record_event WHERE kind='M05.ASSIST_PRESS' AND entity_id=?",
                        String.class,
                        assistAlert(presser)))
                .containsExactly("M05.ALERT");
        mvc.perform(as(floorManager, MockMvcRequestBuilders.get("/api/v1/alerts/{id}", assistAlert(presser))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.origin").value("ASSIST_BUTTON"));
    }
}
