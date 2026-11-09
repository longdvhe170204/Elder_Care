package com.eldercare.backend.incident;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/**
 * BR-M05-27, US12 #1, #3, SC-017 (T067): lần bấm SOS tạo cảnh báo Khẩn cấp khóa {@code SOS} có phòng, giường lúc bấm;
 * lần bấm sau gộp; cùng {@code Idempotency-Key} không tăng số lần; người nhận như FR-047a, không có người liên hệ chính,
 * Quản lý viện.
 */
class BR_M05_27_SosButtonIT extends SosTestBase {

    @Test
    void BR_M05_27_pressCreatesCriticalSosAlertWithRoomAndBed() throws Exception {
        String body = press("k1")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SENT"))
                .andExpect(jsonPath("$.contacts.emergencyNumber").value("115"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        long alertId = ((Number) JsonPath.read(body, "$.alertId")).longValue();
        long pressId = ((Number) JsonPath.read(body, "$.pressId")).longValue();
        var alert = jdbc.queryForMap(
                "SELECT type, level, floor_area_id, occurrence_count, created_by_id FROM alert WHERE id = ?", alertId);
        assertThat(alert.get("type")).isEqualTo("RESIDENT_HELP_REQUEST");
        assertThat(alert.get("level")).isEqualTo("CRITICAL");
        assertThat(((Number) alert.get("floor_area_id")).longValue()).isEqualTo(floor);
        assertThat(((Number) alert.get("occurrence_count")).intValue()).isEqualTo(1);
        assertThat(alert.get("created_by_id")).isNull();
        var press = jdbc.queryForMap(
                "SELECT JSON_VALUE(content, '$.roomId' RETURNING SIGNED) room, JSON_VALUE(content, '$.bedId' RETURNING"
                        + " SIGNED) bed, JSON_VALUE(content, '$.residentStatus') st FROM record_event WHERE id = ?",
                pressId);
        assertThat(((Number) press.get("room")).longValue()).isEqualTo(ROOM);
        assertThat(((Number) press.get("bed")).longValue()).isEqualTo(BED);
        assertThat(press.get("st")).isEqualTo("IN_RESIDENCE");

        mvc.perform(as(nurse, MockMvcRequestBuilders.get("/api/v1/alerts/{id}", alertId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.origin").value("SOS"));
    }

    /** T106: dòng nơi đang ở gắn thẳng hồ sơ người cao tuổi (cha của lần bấm), job lưu giữ xóa cùng hồ sơ, không để mồ côi. */
    @Test
    void T106_placeRowIsAttachedToResident_notToThePressRow() throws Exception {
        String body = press("k-place")
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        long pressId = ((Number) JsonPath.read(body, "$.pressId")).longValue();

        place(pressId, "ROOM").andExpect(status().is2xxSuccessful());

        var row = jdbc.queryForMap(
                "SELECT entity_type, entity_id FROM record_event WHERE kind = 'M05.SOS_PLACE' AND unique_key = ?",
                "SOS_PLACE:" + pressId);
        assertThat(row.get("entity_type")).isEqualTo("M01.RESIDENT");
        assertThat(((Number) row.get("entity_id")).longValue()).isEqualTo(resident);
    }
}
