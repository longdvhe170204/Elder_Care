package com.eldercare.backend.incident.web;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.incident.internal.incident.service.IncidentFloorQueries;
import com.eldercare.backend.incident.web.response.IncidentFloorResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** {@code GET /incident-floors}: tầng chọn được khi ghi sự cố cơ sở vật chất, gửi yêu cầu hỗ trợ (BR-M05-17, 29). */
@RestController
@RequestMapping("/api/v1/incident-floors")
public class IncidentFloorController {
    private final IncidentFloorQueries floors;
    private final CurrentUser currentUser;

    public IncidentFloorController(IncidentFloorQueries floors, CurrentUser currentUser) {
        this.floors = floors;
        this.currentUser = currentUser;
    }

    @GetMapping
    @ApiResponses({@ApiResponse(responseCode = "200", description = "OK")})
    public List<IncidentFloorResponse> list() {
        return floors.floors(currentUser).stream()
                .map(IncidentFloorResponse::of)
                .toList();
    }
}
