package com.eldercare.backend.incident.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.common.Reason;
import com.eldercare.backend.incident.internal.alert.domain.AlertDetails;
import com.eldercare.backend.incident.internal.alert.domain.AlertRecord;
import com.eldercare.backend.incident.internal.alert.domain.AlertStatus;
import com.eldercare.backend.incident.internal.alert.domain.IncidentLevel;
import com.eldercare.backend.incident.internal.alert.service.AlertCommandService;
import com.eldercare.backend.incident.internal.alert.service.AlertHandlingService;
import com.eldercare.backend.incident.internal.alert.service.AlertQueryService;
import com.eldercare.backend.incident.internal.alert.service.ManualAlertService;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/** Route cảnh báo của US3 theo api-007 (T031): đọc, tạo thủ công, các lệnh bảng FR-029. */
class FR_029_AlertControllerTest {
    private static final Instant AT = Instant.parse("2026-11-04T02:00:00Z");

    private final AlertCommandService receive = mock(AlertCommandService.class);
    private final AlertHandlingService handling = mock(AlertHandlingService.class);
    private final ManualAlertService manual = mock(ManualAlertService.class);
    private final AlertQueryService queries = mock(AlertQueryService.class);
    private final CurrentUser user = mock(CurrentUser.class);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(
                    new AlertController(receive, handling, manual, queries, user))
            .build();

    @Test
    void FR_027_detailReturnsAlertWithSourcesAndAllowedCommands() throws Exception {
        when(queries.detail(12, user))
                .thenReturn(new AlertDetails(
                        alert(2),
                        "Điều dưỡng D",
                        List.of(new AlertDetails.Source("M06.VITAL_SIGN_RECORD", 77L, "MEDIUM", AT, null)),
                        Set.of("receive")));

        mvc.perform(get("/api/v1/alerts/12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(12))
                .andExpect(jsonPath("$.typeKey").value("VUOT_NGUONG:5"))
                .andExpect(jsonPath("$.occurrenceCount").value(2))
                .andExpect(jsonPath("$.status").value("ESCALATED"))
                .andExpect(jsonPath("$.maxEscalationReached").value(true))
                .andExpect(jsonPath("$.assignee").value("Điều dưỡng D"))
                .andExpect(jsonPath("$.origin").value("SYSTEM"))
                .andExpect(jsonPath("$.sources[0].sourceId").value(77))
                .andExpect(jsonPath("$.allowedCommands[0]").value("receive"));
    }

    @Test
    void FR_036_listPassesFilters() throws Exception {
        when(queries.list(any(), eq(7L), isNull(), eq(true), eq(user)))
                .thenReturn(List.of(new AlertDetails(alert(1), null, List.of(), Set.of())));

        mvc.perform(get("/api/v1/alerts?status=NEW&status=ESCALATED&floorAreaId=7&maxEscalationReached=true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(12));

        verify(queries).list(List.of(AlertStatus.NEW, AlertStatus.ESCALATED), 7L, null, true, user);
    }

    @Test
    void FR_026_manualCreateReturnsCreatedWithMergedFlag() throws Exception {
        when(manual.create(101, 30, "MEDIUM", "Đi lại liên tục", user))
                .thenReturn(new ManualAlertService.Created(alert(2), true));

        mvc.perform(post("/api/v1/alerts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"residentId":101,"incidentTypeId":30,"level":"MEDIUM","description":"Đi lại liên tục"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.merged").value(true));
    }

    @Test
    void FR_029_commandRoutesCallServices() throws Exception {
        mvc.perform(post("/api/v1/alerts/12/start-handling")).andExpect(status().isOk());
        mvc.perform(post("/api/v1/alerts/12/assign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"staffId\":55,\"note\":\"Giao D2\"}"))
                .andExpect(status().isOk());
        mvc.perform(post("/api/v1/alerts/12/change-level")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newLevel\":\"CRITICAL\",\"reason\":\"Lặp 3 lần\"}"))
                .andExpect(status().isOk());
        mvc.perform(post("/api/v1/alerts/12/close")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resolution\":\"Đã cho nghỉ\"}"))
                .andExpect(status().isOk());

        verify(handling).startHandling(12, user);
        verify(handling).assign(12, 55, "Giao D2", user);
        verify(handling).changeLevel(null, 12, IncidentLevel.CRITICAL, new Reason("Lặp 3 lần"), user);
        verify(handling).close(12, "Đã cho nghỉ", null, user);
    }

    private static AlertRecord alert(int count) {
        return new AlertRecord(
                12,
                101L,
                7L,
                "CHI_SO",
                "VUOT_NGUONG:5",
                IncidentLevel.MEDIUM,
                null,
                count,
                AT,
                AT,
                AlertStatus.ESCALATED,
                2,
                AT.plusSeconds(900),
                AT,
                1,
                800L,
                false,
                null,
                null,
                null,
                null,
                null,
                false,
                null,
                null,
                null,
                false,
                false,
                null,
                null,
                null,
                null,
                null,
                false,
                3);
    }
}
