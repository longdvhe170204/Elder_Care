package com.eldercare.backend.incident.web;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.incident.internal.incident.service.IncidentService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class FR_045_IncidentReceiveControllerTest {
    private final IncidentService incidents = mock(IncidentService.class);
    private final CurrentUser currentUser = mock(CurrentUser.class);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new IncidentController(incidents, currentUser))
            .build();

    @Test
    void FR_045_receiveRouteCallsCommand() throws Exception {
        mvc.perform(post("/api/v1/incidents/41/receive")).andExpect(status().isOk());

        verify(incidents).receive(41, currentUser);
    }
}
