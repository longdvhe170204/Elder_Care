package com.eldercare.backend.incident.web;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.incident.internal.incident.service.IncidentProgressionService;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class FR_045_IncidentProgressionControllerTest {
    private final IncidentProgressionService progressions = mock(IncidentProgressionService.class);
    private final CurrentUser currentUser = mock(CurrentUser.class);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(
                    new IncidentProgressionController(progressions, currentUser))
            .build();

    @Test
    void FR_045_progressionRouteCallsCommand() throws Exception {
        mvc.perform(post("/api/v1/incidents/41/progressions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"progressionTypeId":5,"content":"Đã sơ cứu","occurredAt":"2026-11-04T01:58:00Z"}
                                """))
                .andExpect(status().isCreated());

        verify(progressions).add(41, 5, "Đã sơ cứu", Instant.parse("2026-11-04T01:58:00Z"), currentUser);
    }
}
