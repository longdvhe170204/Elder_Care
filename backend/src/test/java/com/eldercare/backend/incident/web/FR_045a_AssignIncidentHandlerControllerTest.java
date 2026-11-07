package com.eldercare.backend.incident.web;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.incident.internal.incident.service.IncidentAssignmentService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class FR_045a_AssignIncidentHandlerControllerTest {
    private final IncidentAssignmentService assignments = mock(IncidentAssignmentService.class);
    private final CurrentUser currentUser = mock(CurrentUser.class);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(
                    new IncidentAssignmentController(assignments, currentUser))
            .build();

    @Test
    void FR_045a_assignRouteCallsCommand() throws Exception {
        mvc.perform(post("/api/v1/incidents/41/assign-handler")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"assigneeId\":700,\"note\":\"Điều dưỡng gần nhất\"}"))
                .andExpect(status().isOk());

        verify(assignments).assign(41, 700, "Điều dưỡng gần nhất", currentUser);
    }
}
