package com.eldercare.backend.incident.web;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.incident.internal.incident.service.IncidentResidentQueries;
import com.eldercare.backend.incident.web.response.IncidentResidentResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** {@code GET /incident-residents?q=}: tìm người cao tuổi khi báo khẩn, trong toàn viện (đề xuất 2026-11-23). */
@RestController
@RequestMapping("/api/v1/incident-residents")
public class IncidentResidentController {
    private final IncidentResidentQueries residents;
    private final CurrentUser currentUser;

    public IncidentResidentController(IncidentResidentQueries residents, CurrentUser currentUser) {
        this.residents = residents;
        this.currentUser = currentUser;
    }

    @GetMapping
    @ApiResponses({@ApiResponse(responseCode = "200", description = "OK")})
    public List<IncidentResidentResponse> search(@RequestParam("q") String query) {
        return residents.search(query, currentUser).stream()
                .map(IncidentResidentResponse::of)
                .toList();
    }
}
