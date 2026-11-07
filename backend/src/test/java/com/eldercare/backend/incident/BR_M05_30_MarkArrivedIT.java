package com.eldercare.backend.incident;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

/**
 * Mốc "Đã tới nơi" của cảnh báo SOS (BR-M05-30; FR-047j; quickstart S12; T094): ghi một lần, trạng thái giữ nguyên,
 * kênh người cao tuổi hiện "đã tới"; cảnh báo đóng mà chưa ghi mốc mang dấu "không ghi mốc tới nơi".
 */
class BR_M05_30_MarkArrivedIT extends SosTestBase {
    @Test
    void BR_M05_30_markArrivedOnce_statusUnchanged() throws Exception {
        press("arr-1").andExpect(status().isCreated());
        long alertId = sosAlert();
        post(nurse, "/api/v1/alerts/{id}/receive", alertId, "{}").andExpect(status().isOk());

        post(nurse, "/api/v1/alerts/{id}/mark-arrived", alertId, "{}").andExpect(status().isOk());
        post(nurse, "/api/v1/alerts/{id}/mark-arrived", alertId, "{}").andExpect(status().isConflict());

        assertThat(jdbc.queryForObject("SELECT status FROM alert WHERE id = ?", String.class, alertId))
                .isEqualTo("RECEIVED");
        current().andExpect(jsonPath("$.status").value("ARRIVED"));
    }

    @Test
    void FR_047j_closeWithoutArrivalMarksSkipped() throws Exception {
        press("arr-2").andExpect(status().isCreated());
        long alertId = sosAlert();
        handleAndClose(alertId);

        mvc.perform(as(nurse, get("/api/v1/alerts/{id}", alertId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.skipArrivalTime").value(true));
    }

    @Test
    void FR_047j_closeAfterArrivalIsNotSkipped() throws Exception {
        press("arr-3").andExpect(status().isCreated());
        long alertId = sosAlert();
        post(nurse, "/api/v1/alerts/{id}/receive", alertId, "{}").andExpect(status().isOk());
        post(nurse, "/api/v1/alerts/{id}/mark-arrived", alertId, "{}").andExpect(status().isOk());
        post(nurse, "/api/v1/alerts/{id}/start-handling", alertId, "{}").andExpect(status().isOk());
        post(nurse, "/api/v1/alerts/{id}/close", alertId, "{\"resolution\":\"Đã hỗ trợ\"}")
                .andExpect(status().isOk());

        assertThat(jdbc.queryForObject("SELECT skip_arrival_time FROM alert WHERE id = ?", Boolean.class, alertId))
                .isFalse();
    }

    private void handleAndClose(long alertId) throws Exception {
        post(nurse, "/api/v1/alerts/{id}/receive", alertId, "{}").andExpect(status().isOk());
        post(nurse, "/api/v1/alerts/{id}/start-handling", alertId, "{}").andExpect(status().isOk());
        post(nurse, "/api/v1/alerts/{id}/close", alertId, "{\"resolution\":\"Đã hỗ trợ\"}")
                .andExpect(status().isOk());
    }
}
