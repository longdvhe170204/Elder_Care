package com.eldercare.backend.incident.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.incident.internal.emergency.service.EmergencyCardService;
import com.eldercare.backend.incident.internal.incident.domain.RecordedIncident;
import com.eldercare.backend.incident.internal.incident.service.IncidentRecordService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class FR_040_IncidentRecordControllerTest {
    private final IncidentRecordService incidents = mock(IncidentRecordService.class);
    private final EmergencyCardService cards = mock(EmergencyCardService.class);
    private final CurrentUser currentUser = mock(CurrentUser.class);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(
                    new IncidentRecordController(incidents, cards, currentUser))
            .build();

    @Test
    void FR_048_outsideScopeRecorderOnlyGetsIdentification() throws Exception {
        when(incidents.record(any(), eq(currentUser))).thenReturn(new RecordedIncident(41L, true, false));

        mvc.perform(post("/api/v1/incidents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"residentId":101,"incidentTypeId":2,"discoveredAt":"2026-11-04T01:00:00Z",
                                 "description":"Ngã","level":"MEDIUM"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.incident.id").value(41))
                .andExpect(jsonPath("$.incident.content").doesNotExist())
                .andExpect(jsonPath("$.identificationOnly").value(true));
    }
}
