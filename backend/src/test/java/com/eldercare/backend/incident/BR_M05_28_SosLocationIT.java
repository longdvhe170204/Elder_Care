package com.eldercare.backend.incident;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.resident.api.dto.ResidentStatus;
import com.eldercare.backend.support.TestUsers;
import com.eldercare.backend.support.TestUsers.TestUser;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/**
 * Vị trí GPS của cảnh báo từ nút SOS, nút hỗ trợ (BR-M05-28; FR-047h; quickstart S9 → S11; T094): lệnh bấm không chờ vị
 * trí; mỗi lần gửi lưu một dòng; người nhận thấy tọa độ mới nhất, quá hai chu kỳ CFG-M05-09 mang "có thể đã cũ"; Hoạt
 * động bên ngoài thì tọa độ là vị trí chính; đã tới nơi thì app dừng gửi (409 {@code STOP_SENDING_LOCATION}).
 */
class BR_M05_28_SosLocationIT extends AssistTestBase {
    static final String LOCATION = "/api/v1/sos/{id}/location";

    @Test
    void BR_M05_28_pressDoesNotWaitForLocation_appIsAskedToSend() throws Exception {
        press("loc-1")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sendLocation").value(true))
                .andExpect(jsonPath("$.locationIntervalSeconds").value(30));
        assertThat(locations(sosAlert())).isZero();
    }

    @Test
    void BR_M05_28_eachReportIsStored_staffSeesLatestAsReference() throws Exception {
        press("loc-2").andExpect(status().isCreated());
        long alertId = sosAlert();

        report(pressId(), time.now().minusSeconds(20), "10.1").andExpect(status().isNoContent());
        report(pressId(), time.now().minusSeconds(5), "10.2").andExpect(status().isNoContent());

        assertThat(locations(alertId)).isEqualTo(2);
        detail(alertId)
                .andExpect(jsonPath("$.latestLocation.latitude").value(10.2))
                .andExpect(jsonPath("$.latestLocation.accuracyMeters").value(12.5))
                .andExpect(jsonPath("$.latestLocation.possiblyStale").value(false))
                .andExpect(jsonPath("$.latestLocation.isPrimary").value(false));
    }

    @Test
    void FR_047h_locationOlderThanTwoIntervalsIsPossiblyStale() throws Exception {
        press("loc-3").andExpect(status().isCreated());
        long alertId = sosAlert();

        report(pressId(), time.now().minusSeconds(61), "10.3").andExpect(status().isNoContent());

        detail(alertId).andExpect(jsonPath("$.latestLocation.possiblyStale").value(true));
    }

    @Test
    void FR_047h_offSiteActivityMakesLocationPrimary() throws Exception {
        Mockito.when(residents.status(resident)).thenReturn(ResidentStatus.OFF_SITE_ACTIVITY);
        press("loc-4").andExpect(status().isCreated());
        long alertId = sosAlert();

        report(pressId(), time.now(), "10.4").andExpect(status().isNoContent());

        detail(alertId).andExpect(jsonPath("$.latestLocation.isPrimary").value(true));
    }

    @Test
    void BR_M05_30_arrivedStopsLocation() throws Exception {
        press("loc-5").andExpect(status().isCreated());
        long alertId = sosAlert();
        post(nurse, "/api/v1/alerts/{id}/receive", alertId, "{}").andExpect(status().isOk());
        post(nurse, "/api/v1/alerts/{id}/mark-arrived", alertId, "{}").andExpect(status().isOk());

        report(pressId(), time.now(), "10.5")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("STOP_SENDING_LOCATION"));
        current().andExpect(jsonPath("$.sendLocation").value(false));
        assertThat(locations(alertId)).isZero();
    }

    @Test
    void FR_047h_invalidCoordinatesAndOtherResidentsPressAreRejected() throws Exception {
        press("loc-6").andExpect(status().isCreated());
        long pressId = pressId();

        report(pressId, time.now(), "91").andExpect(status().is4xxClientError());

        String otherToken = bindDevice(residentAccount(TestUsers.nextId()), "dev-other-" + pressId);
        mvc.perform(MockMvcRequestBuilders.post(LOCATION, pressId)
                        .header("X-Device-Token", otherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(time.now(), "10.6")))
                .andExpect(status().isNotFound());
        assertThat(locations(sosAlert())).isZero();
    }

    @Test
    void FR_047h_assistButtonPresserSendsLocation_othersCannot() throws Exception {
        pressAssist(presser).andExpect(status().isCreated());
        long alertId = assistAlert(presser);

        assistReport(presser, alertId).andExpect(status().isNoContent());
        assistReport(nurse, alertId).andExpect(status().isNotFound());

        assertThat(locations(alertId)).isEqualTo(1);
        mvc.perform(as(floorManager, get("/api/v1/alerts/{id}", alertId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.latestLocation.isPrimary").value(false));
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
