package com.eldercare.backend.family;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.common.Role;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;

/**
 * 012 T062 (US9; FR-084, BR-M10-11; quickstart K2; SC-011): kênh không có trường sức khỏe, chi phí, số dư, hợp đồng;
 * chỉ tài khoản người cao tuổi mở được kênh; người cao tuổi không mở được dữ liệu của người khác qua cổng hay phản hồi.
 */
class FR_084_ResidentChannelNoLeakIT extends ResidentChannelTestBase {

    private static final List<String> SECTIONS =
            List.of("today", "menu", "activities", "visits", "belongings", "feedback");
    private static final List<String> FORBIDDEN =
            List.of("health", "medication", "allerg", "diet", "condition", "cost", "balance", "contract", "deposit");

    @Test
    void FR_084_noForbiddenFields() throws Exception {
        for (String section : SECTIONS) {
            String body = mvc.perform(get("/api/v1/resident-channel/" + section).header("Authorization", self()))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString(StandardCharsets.UTF_8)
                    .toLowerCase(Locale.ROOT);
            for (String word : FORBIDDEN) {
                assertThat(body).as(section + " có " + word).doesNotContain(word);
            }
        }
    }

    @Test
    void FR_084_onlyResidentAccountsOpenChannel() throws Exception {
        for (String authorization : List.of(family(mMember), office, staffSession(Role.NURSING_HOME_MANAGER))) {
            mvc.perform(get("/api/v1/resident-channel/today").header("Authorization", authorization))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void FR_084_cannotOpenOtherResident() throws Exception {
        long b = resident(ResidentStatus.IN_RESIDENCE);
        mvc.perform(get("/api/v1/family-portal/residents/" + b + "/general").header("Authorization", self()))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/family-portal/residents/" + a + "/general").header("Authorization", self()))
                .andExpect(status().isForbidden());
        // Không có thao tác đăng ký thăm trên kênh (FR-084)
        postAs(self(), "/api/v1/visits", registration(a, null, day.plusDays(1), MORNING_START, MORNING_END))
                .andExpect(status().isForbidden());
    }
}
