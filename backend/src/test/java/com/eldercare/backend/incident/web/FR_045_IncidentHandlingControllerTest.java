package com.eldercare.backend.incident.web;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.common.Reason;
import com.eldercare.backend.incident.internal.incident.service.IncidentHandlingService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class FR_045_IncidentHandlingControllerTest {
    private final IncidentHandlingService handling = mock(IncidentHandlingService.class);
    private final CurrentUser currentUser = mock(CurrentUser.class);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new IncidentHandlingController(handling, currentUser))
            .build();

    @Test
    void FR_045_lifecycleRoutesCallCommands() throws Exception {
        mvc.perform(post("/api/v1/incidents/41/move-to-monitoring")).andExpect(status().isOk());
        verify(handling).moveToMonitoring(41, currentUser);

        mvc.perform(post("/api/v1/incidents/41/reopen")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Diễn biến xấu\"}"))
                .andExpect(status().isOk());
        verify(handling).reopen(null, 41, new Reason("Diễn biến xấu"), currentUser);

        mvc.perform(post("/api/v1/incidents/41/close")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resolution\":\"Đã cách ly\",\"infectionOutcome\":\"RULED_OUT\"}"))
                .andExpect(status().isOk());
        verify(handling).close(41, "Đã cách ly", "RULED_OUT", currentUser);

        mvc.perform(post("/api/v1/incidents/41/join-handling")).andExpect(status().isOk());
        verify(handling).join(41, currentUser);
    }
}
