package com.eldercare.backend.administration.parameter;

import static com.eldercare.backend.administration.parameter.ParameterFixtures.MANAGER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.administration.parameter.api.ParameterApi;
import com.eldercare.backend.administration.parameter.api.dto.ParameterCode;
import com.eldercare.backend.common.time.TestClockControl;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.SwitchableUser;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

/**
 * US4 #8; SC-005, FR-019 (000 T078; quickstart K5): giá trị tại một thời điểm quá khứ là giá trị hiện hành lúc đó, qua
 * {@code ParameterApi.readAt} và {@code GET /parameters/{code}/history?asOf=}.
 */
@AutoConfigureMockMvc
@WithMockUser
@Import(ParameterFixtures.class)
class ParameterAsOfIT extends MySqlIntegrationTestBase {

    // Mã riêng của test này: đồng hồ giả lập đặt mốc tương lai, mã dùng chung với test khác sẽ lệch lịch sử
    private static final String RECHECK = "CFG-M06-04";

    @Autowired
    ParameterApi parameters;

    @Autowired
    TestClockControl clock;

    @Autowired
    MockMvc mvc;

    @Autowired
    SwitchableUser user;

    private ParameterClient api;

    @BeforeEach
    void setUp() {
        api = new ParameterClient(mvc);
        user.set(MANAGER);
    }

    @AfterEach
    void restore() throws Exception {
        // Khôi phục trước khi trả đồng hồ, để dòng lịch sử mới nhất là dòng khôi phục
        api.restoreDefault(RECHECK, api.etag(RECHECK), "khôi phục sau test");
        clock.reset();
    }

    @Test
    void US4_8_valueAtPastInstant_isTheValueCurrentThen() throws Exception {
        // Mốc tương đối theo giờ thật: sau dòng nạp ban đầu của V000_04 (UTC_TIMESTAMP lúc chạy Flyway); ba mốc
        // trong cùng một ngày để dòng sót của lần chạy hôm trước (schema test giữ dữ liệu) luôn trước firstChange
        Instant day = Instant.now().truncatedTo(ChronoUnit.DAYS).plus(Duration.ofDays(30));
        Instant firstChange = day.plus(Duration.ofHours(1));
        Instant between = day.plus(Duration.ofHours(2));
        Instant secondChange = day.plus(Duration.ofHours(3));
        clock.setTo(firstChange);
        api.change(RECHECK, api.etag(RECHECK), "{\"value\":20,\"unit\":\"MINUTES\"}", "Lần 1")
                .andExpect(status().isOk());
        clock.setTo(secondChange);
        api.change(RECHECK, api.etag(RECHECK), "{\"value\":30,\"unit\":\"MINUTES\"}", "Lần 2")
                .andExpect(status().isOk());

        assertThat(parameters.readAt(ParameterCode.CFG_M06_04, between).asDuration())
                .isEqualTo(Duration.ofMinutes(20));
        assertThat(parameters
                        .readAt(ParameterCode.CFG_M06_04, firstChange.minusSeconds(1))
                        .asDuration())
                .isEqualTo(Duration.ofMinutes(10));
        mvc.perform(get(ParameterClient.URL + RECHECK + "/history")
                        .param("asOf", between.atOffset(ZoneOffset.ofHours(7)).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].value.value").value(20))
                .andExpect(jsonPath("$[0].reason").value("Lần 1"));
    }
}
