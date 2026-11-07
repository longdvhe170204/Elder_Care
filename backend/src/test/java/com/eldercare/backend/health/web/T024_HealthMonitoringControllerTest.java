package com.eldercare.backend.health.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.common.Reason;
import com.eldercare.backend.health.internal.schedule.service.MeasurementScheduleService;
import com.eldercare.backend.health.internal.threshold.service.ThresholdService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/** Route ngưỡng và lịch đo của T024 gọi đúng lệnh (FR-003 → FR-006, FR-020 → FR-025). */
class T024_HealthMonitoringControllerTest {
    private final MeasurementScheduleService schedules = mock(MeasurementScheduleService.class);
    private final ThresholdService thresholds = mock(ThresholdService.class);
    private final CurrentUser currentUser = mock(CurrentUser.class);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(
                    new MeasurementScheduleController(schedules, currentUser),
                    new ThresholdController(thresholds, currentUser))
            .build();

    @Test
    void FR_003_scheduleRoutes() throws Exception {
        mvc.perform(post("/api/v1/residents/101/measurement-schedules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"indicatorIds":[3],"frequency":{"type":"CLOCK_TIMES","times":["06:00"]},
                                 "startAt":"2026-11-04T00:00:00Z"}
                                """))
                .andExpect(status().isCreated());
        verify(schedules).create(eq(101L), any(), eq(currentUser));

        mvc.perform(post("/api/v1/measurement-schedules/50/stop")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Không cần đo\"}"))
                .andExpect(status().isOk());
        verify(schedules).stop(50, new Reason("Không cần đo"), currentUser);

        when(schedules.occurrences(50, Instant.parse("2026-11-04T00:00:00Z"), Instant.parse("2026-11-05T00:00:00Z")))
                .thenReturn(List.of(Instant.parse("2026-11-04T23:00:00Z")));
        mvc.perform(get("/api/v1/measurement-schedules/50/measurement-times")
                        .param("from", "2026-11-04T00:00:00Z")
                        .param("to", "2026-11-05T00:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("2026-11-04T23:00:00Z"));

        mvc.perform(get("/api/v1/residents/101/measurement-schedules")).andExpect(status().isOk());
        verify(schedules).list(new Target("M01.RESIDENT", "101", 101L, null));
    }

    @Test
    void FR_022_thresholdRoutes() throws Exception {
        mvc.perform(post("/api/v1/thresholds")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"scope":"FACILITY","indicatorId":3,"effectiveFrom":"2026-11-04T00:00:00Z",
                                 "components":[{"code":"VALUE","warningAbove":37.5}]}
                                """))
                .andExpect(status().isCreated());
        verify(thresholds).set(any(), eq(currentUser));

        mvc.perform(post("/api/v1/thresholds/9/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Nhập nhầm\"}"))
                .andExpect(status().isOk());
        verify(thresholds).cancel(9, new Reason("Nhập nhầm"), currentUser);

        when(thresholds.missingPersonal()).thenReturn(List.of(101L));
        mvc.perform(get("/api/v1/vital-signs/missing-personal-threshold"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value(101));
    }
}
