package com.eldercare.backend.incident;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.care.api.dto.AttendanceStatus;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import java.sql.Timestamp;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * FR-047d, US12 #5 (T068): không ở viện → 409 {@code NOT_AT_FACILITY}; bán trú không Có mặt → 409 {@code NOT_PRESENT};
 * token sai, thiết bị đã gỡ, tài khoản Đã khóa → 401 {@code INVALID_DEVICE}; mọi lỗi kèm {@code contacts}; lần bấm bị từ
 * chối không ghi và không tạo cảnh báo.
 */
class FR_047d_SosLocationConditionIT extends SosTestBase {

    @ParameterizedTest
    @EnumSource(
            value = ResidentStatus.class,
            names = {"TEMPORARILY_ABSENT", "HOSPITALIZED", "RESIDENCY_ENDED", "DECEASED"})
    void FR_047d_notAtFacilityRefused(ResidentStatus residentStatus) throws Exception {
        when(residents.status(resident)).thenReturn(residentStatus);

        press("k1")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("NOT_AT_FACILITY"))
                .andExpect(jsonPath("$.contacts.emergencyNumber").value("115"));

        assertThat(sosAlerts()).isZero();
        assertThat(presses()).isZero();
    }

    @Test
    void FR_047d_offSiteActivityCanPress() throws Exception {
        when(residents.status(resident)).thenReturn(ResidentStatus.OFF_SITE_ACTIVITY);

        press("k1").andExpect(status().isCreated());
    }

    @Test
    void FR_047d_dayCareNotPresentRefused() throws Exception {
        dayCare(AttendanceStatus.NOT_ARRIVED);
        press("k1")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("NOT_PRESENT"))
                .andExpect(jsonPath("$.contacts.emergencyNumber").value("115"));

        dayCare(AttendanceStatus.LEFT);
        press("k2")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("NOT_PRESENT"));

        dayCare(null);
        press("k3")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("NOT_PRESENT"));
        assertThat(sosAlerts()).isZero();
    }

    @Test
    void FR_047d_dayCarePresentCanPress() throws Exception {
        dayCare(AttendanceStatus.PRESENT);

        press("k1").andExpect(status().isCreated());
    }

    @Test
    void FR_047d_wrongTokenOrMissingHeaderUnauthorized() throws Exception {
        press("sai-" + deviceToken, "k1")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("INVALID_DEVICE"))
                .andExpect(jsonPath("$.contacts.emergencyNumber").value("115"));
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(SOS))
                .andExpect(status().isUnauthorized());
        mvc.perform(get(CURRENT).header("X-Device-Token", "sai")).andExpect(status().isUnauthorized());
        assertThat(sosAlerts()).isZero();
    }

    @Test
    void FR_047d_unboundDeviceUnauthorized() throws Exception {
        jdbc.update(
                "UPDATE device_credential SET ended_at = ?, end_reason = 'NEW_DEVICE_BOUND' WHERE account_id = ?"
                        + " AND kind = 'BOUND_DEVICE'",
                Timestamp.from(time.now()),
                residentAccount);

        press("k1")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("INVALID_DEVICE"));
    }

    @Test
    void FR_047d_lockedAccountUnauthorized() throws Exception {
        jdbc.update("UPDATE account SET status = 'LOCKED', lock_cause = 'MANUAL' WHERE id = ?", residentAccount);

        press("k1")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("INVALID_DEVICE"));
    }
}
