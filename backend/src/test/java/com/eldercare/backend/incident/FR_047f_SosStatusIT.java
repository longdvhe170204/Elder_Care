package com.eldercare.backend.incident;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;

/**
 * FR-047f, US12 #4 (T070): {@code GET /sos/current} trả {@code SENT} rồi {@code RECEIVED} kèm họ tên người tiếp nhận;
 * không có cảnh báo SOS đang mở thì 204; nơi đang ở đặt một lần cho mỗi lần bấm.
 */
class FR_047f_SosStatusIT extends SosTestBase {

    @Test
    void FR_047f_noOpenSosAlertNoContent() throws Exception {
        current().andExpect(status().isNoContent());
    }

    @Test
    void FR_047f_sentThenReceivedWithReceiverName() throws Exception {
        long pressId = pressId(press("k1"));
        current()
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SENT"))
                .andExpect(jsonPath("$.pressId").value(pressId))
                .andExpect(jsonPath("$.alertId").value(sosAlert()));

        post(nurse, "/api/v1/alerts/{id}/receive", sosAlert(), "{}").andExpect(status().isOk());

        current()
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RECEIVED"))
                .andExpect(jsonPath("$.receiverName").value("Điều Dưỡng Hoa"));
    }

    @Test
    void FR_047f_currentShowsLatestPressOfMergedAlert() throws Exception {
        press("k1");
        long second = pressId(press("k2"));

        current().andExpect(jsonPath("$.pressId").value(second));
    }

    @Test
    void FR_047f_placeSetOnce() throws Exception {
        long pressId = pressId(press("k1"));

        place(pressId, "RESTROOM").andExpect(status().isNoContent());
        place(pressId, "ROOM").andExpect(status().isConflict());

        assertThat(jdbc.queryForObject(
                        "SELECT JSON_VALUE(content, '$.place') FROM record_event WHERE kind = 'M05.SOS_PLACE'"
                                + " AND entity_id = ?",
                        String.class,
                        pressId))
                .isEqualTo("RESTROOM");
    }

    @Test
    void FR_047f_placeOfAnotherResidentsPressNotFound() throws Exception {
        long pressId = pressId(press("k1"));
        long other = com.eldercare.backend.support.TestUsers.nextId();
        long otherAccount = residentAccount(other);
        deviceToken = bindDevice(otherAccount, "dev-" + other);

        place(pressId, "ROOM").andExpect(status().isNotFound());
    }

    private static long pressId(org.springframework.test.web.servlet.ResultActions result) throws Exception {
        String body =
                result.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.pressId")).longValue();
    }
}
