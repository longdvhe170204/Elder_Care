package com.cambae.backend.common.time.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.common.time.TestClockControl;
import com.cambae.backend.support.MySqlIntegrationTestBase;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
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
