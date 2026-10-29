package com.eldercare.backend.common.time.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.common.time.TestClockControl;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

/** NFR-13 (000 T012): đặt, tua đồng hồ giả lập qua /api/v1/testing/clock; BusinessTime đọc theo đồng hồ này. */
@AutoConfigureMockMvc
@WithMockUser
class TestClockControllerIT extends MySqlIntegrationTestBase {

    @Autowired
    MockMvc mvc;

    @Autowired
    TestClockControl clock;

    @Autowired
    BusinessTime time;

    @Autowired
    JdbcTemplate jdbc;

    @AfterEach
    void resetClock() {
        clock.reset();
    }

    @Test
    void NFR_13_readClock_notSimulatedByDefault() throws Exception {
        mvc.perform(get("/api/v1/testing/clock"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.simulated").value(false));
    }

    @Test
    void NFR_13_setTo_movesBusinessTime() throws Exception {
        mvc.perform(post("/api/v1/testing/clock")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"setTo\":\"2026-12-31T23:59:00+07:00\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.simulated").value(true));

        assertThat(time.today()).hasToString("2026-12-31");
        assertThat(Duration.between(Instant.parse("2026-12-31T16:59:00Z"), time.now()))
                .isBetween(Duration.ZERO, Duration.ofSeconds(30));
    }

    @Test
    void NFR_13_runJobs_runsM13FastAndDailyAtSimulatedTime() throws Exception {
        // 009: hai job của M13 chạy tay được ở profile test, demo (chủ dự án duyệt thêm vào runJobs 2026-10-22)
        int fastBefore = runs("M13_FAST");

        mvc.perform(post("/api/v1/testing/clock")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"setTo\":\"2031-03-17T08:00:00+07:00\",\"runJobs\":[\"M13_FAST\",\"M13_DAILY\"]}"))
                .andExpect(status().isOk());

        assertThat(runs("M13_FAST")).isGreaterThan(fastBefore);
        // Job hằng ngày chạy một lần mỗi ngày nghiệp vụ: đã có lần chạy cho ngày giả lập
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM scheduler_run WHERE job = 'M13_DAILY'"
                                + " AND started_at >= '2031-03-17 00:30:00' AND started_at < '2031-03-18 00:00:00'",
                        Integer.class))
                .isEqualTo(1);
    }

    @Test
    void NFR_13_runJobs_runsM03HoldExpiryAndFutureAllocation() throws Exception {
        // 003 (FR-015, FR-023, research G-09): hai job của M03 chạy tay được (chủ dự án duyệt thêm vào runJobs
        // 2026-10-26)
        int holdsBefore = runs("EXPIRE_BED_HOLDS");
        int futureBefore = runs("REPORT_FUTURE_ALLOCATION_PAST_START");

        mvc.perform(post("/api/v1/testing/clock")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"runJobs\":[\"EXPIRE_BED_HOLDS\",\"REPORT_FUTURE_ALLOCATION_PAST_START\"]}"))
                .andExpect(status().isOk());

        assertThat(runs("EXPIRE_BED_HOLDS")).isGreaterThan(holdsBefore);
        assertThat(runs("REPORT_FUTURE_ALLOCATION_PAST_START")).isGreaterThan(futureBefore);
    }

    private int runs(String job) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM scheduler_run WHERE job = ?", Integer.class, job);
    }

    @Test
    void NFR_13_advanceBy_addsDuration() throws Exception {
        Instant before = time.now();

        mvc.perform(post("/api/v1/testing/clock")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"advanceBy\":\"PT49H\"}"))
                .andExpect(status().isOk());

        assertThat(Duration.between(before, time.now()))
                .isBetween(Duration.ofHours(49), Duration.ofHours(49).plusSeconds(30));
    }
}
