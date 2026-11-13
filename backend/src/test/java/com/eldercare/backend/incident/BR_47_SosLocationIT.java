package com.eldercare.backend.incident;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.support.TestUsers.TestUser;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/**
 * Vị trí GPS của cảnh báo từ nút SOS, nút hỗ trợ (BR-M05-28; FR-047h; quickstart S9 → S11; T094): lệnh bấm không chờ vị
 * trí; mỗi lần gửi lưu một dòng; người nhận thấy tọa độ mới nhất, quá hai chu kỳ CFG-M05-09 mang "có thể đã cũ"; Hoạt
 * động bên ngoài thì tọa độ là vị trí chính; đã tới nơi thì app dừng gửi (409 {@code STOP_SENDING_LOCATION}).
 */
class BR_47_SosLocationIT extends AssistTestBase {
    static final String LOCATION = "/api/v1/sos/{id}/location";

    @Test
    void BR_47_pressDoesNotWaitForLocation_appIsAskedToSend() throws Exception {
        press("loc-1")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sendLocation").value(true))
                .andExpect(jsonPath("$.locationIntervalSeconds").value(30));
        assertThat(locations(sosAlert())).isZero();
    }

    private ResultActions report(long pressId, Instant measuredAt, String latitude) throws Exception {
        return mvc.perform(MockMvcRequestBuilders.post(LOCATION, pressId)
                .header("X-Device-Token", deviceToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(measuredAt, latitude)));
    }

    private ResultActions assistReport(TestUser as, long alertId) throws Exception {
        return post(as, ASSIST + "/{id}/location", alertId, body(time.now(), "10.7"));
    }

    private ResultActions detail(long alertId) throws Exception {
        return mvc.perform(as(nurse, get("/api/v1/alerts/{id}", alertId))).andExpect(status().isOk());
    }

    private static String body(Instant measuredAt, String latitude) {
        return "{\"latitude\":" + latitude + ",\"longitude\":106.7,\"accuracyMeters\":12.5,\"measuredAt\":\""
                + measuredAt + "\"}";
    }

    private long pressId() {
        return jdbc.queryForObject(
                "SELECT MAX(id) FROM record_event WHERE kind = 'M05.SOS_PRESS' AND entity_id = ?",
                Long.class,
                resident);
    }

    private int locations(long alertId) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM record_event WHERE kind = 'M05.ALERT_LOCATION' AND entity_id = ?",
                Integer.class,
                alertId);
    }
}
