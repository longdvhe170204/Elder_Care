package com.eldercare.backend.incident.web;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.incident.internal.alert.service.AlertCommandService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class FR_029_AlertReceiveControllerTest {
    private final AlertCommandService alerts = mock(AlertCommandService.class);
    private final CurrentUser currentUser = mock(CurrentUser.class);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new AlertController(
                    alerts,
                    mock(com.eldercare.backend.incident.internal.alert.service.AlertHandlingService.class),
                    mock(com.eldercare.backend.incident.internal.alert.service.ManualAlertService.class),
                    mock(com.eldercare.backend.incident.internal.alert.service.AlertQueryService.class),
                    currentUser))
            .build();

    @Test
    void FR_029_receiveRouteCallsCommand() throws Exception {
        mvc.perform(post("/api/v1/alerts/12/receive")).andExpect(status().isOk());

        verify(alerts).receive(12, currentUser);
    }
}
