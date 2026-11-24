package com.eldercare.backend.incident.web;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.incident.internal.incident.service.IncidentFollowUpQueries;
import com.eldercare.backend.incident.web.response.IncidentFollowUpTaskResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** {@code GET /incidents/{id}/follow-up-tasks}: việc tiếp theo và kết quả trong hồ sơ sự cố (FR-045b). */
@RestController
@RequestMapping("/api/v1/incidents")
public class IncidentFollowUpController {
    private final IncidentFollowUpQueries followUps;
    private final CurrentUser currentUser;

    public IncidentFollowUpController(IncidentFollowUpQueries followUps, CurrentUser currentUser) {
        this.followUps = followUps;
        this.currentUser = currentUser;
    }

    @GetMapping("/{id}/follow-up-tasks")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "OK")})
    public List<IncidentFollowUpTaskResponse> list(@PathVariable long id) {
        return followUps.ofIncident(id, currentUser).stream()
                .map(IncidentFollowUpTaskResponse::of)
                .toList();
    }
}
