package com.eldercare.backend.incident.web;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.common.Reason;
import com.eldercare.backend.incident.internal.alert.domain.IncidentLevel;
import com.eldercare.backend.incident.internal.incident.service.IncidentService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class BR_M05_22_IncidentLevelControllerTest {
    private final IncidentService incidents = mock(IncidentService.class);
    private final CurrentUser currentUser = mock(CurrentUser.class);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new IncidentController(incidents, currentUser))
            .build();

    @Test
    void BR_M05_22_changeLevelRouteCallsBusinessCommand() throws Exception {
        mvc.perform(post("/api/v1/incidents/41/change-level")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"newLevel":"MEDIUM","reason":"Đã ổn định"}
                                """))
                .andExpect(status().isOk());

        verify(incidents).changeLevel(41, IncidentLevel.MEDIUM, new Reason("Đã ổn định"), currentUser);
    }
}
