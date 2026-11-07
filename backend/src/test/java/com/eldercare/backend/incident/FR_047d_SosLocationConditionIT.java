package com.eldercare.backend.incident;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.resident.api.dto.ResidentStatus;
import com.eldercare.backend.support.TableRows;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * FR-047d, US12 #5 (T068): không ở viện → 409 {@code NOT_AT_FACILITY}; bán trú không Có mặt → 409 {@code NOT_PRESENT};
 * token sai, thiết bị đã gỡ, tài khoản Đã khóa → 401 {@code INVALID_DEVICE}; mọi lỗi kèm {@code contacts}; lần bấm bị từ
 * chối không ghi và không tạo cảnh báo.
 */
class FR_047d_SosLocationConditionIT extends SosTestBase {

    @Test
    void FR_047d_notAtFacilityRefused() {
        TableRows.each(
                Stream.of(
                        ResidentStatus.TEMPORARILY_ABSENT,
                        ResidentStatus.HOSPITALIZED,
                        ResidentStatus.RESIDENCY_ENDED,
                        ResidentStatus.DECEASED),
                this::refused);
    }

    private void refused(ResidentStatus residentStatus) throws Exception {
        when(residents.status(resident)).thenReturn(residentStatus);

        press("k1")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("NOT_AT_FACILITY"))
                .andExpect(jsonPath("$.contacts.emergencyNumber").value("115"));

        assertThat(sosAlerts()).isZero();
        assertThat(presses()).isZero();
    }
}
