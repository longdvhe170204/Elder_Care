package com.eldercare.backend.incident.web;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.incident.internal.incident.service.IncidentService;
import com.eldercare.backend.incident.web.request.ChangeIncidentLevelRequest;
import com.eldercare.backend.incident.web.response.M05ProblemBody;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Các lệnh vòng đời sự cố. */
@RestController
@RequestMapping("/api/v1/incidents")
public class IncidentController {
    private static final String PROBLEM = "application/problem+json";

    private final IncidentService incidents;
    private final CurrentUser currentUser;

    public IncidentController(IncidentService incidents, CurrentUser currentUser) {
        this.incidents = incidents;
        this.currentUser = currentUser;
    }

    @PostMapping("/{id}/receive")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Đã tiếp nhận"),
        @ApiResponse(
                responseCode = "403",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = M05ProblemBody.class))),
        @ApiResponse(
                responseCode = "409",
                content =
                        @Content(mediaType = PROBLEM, schema = @Schema(implementation = M05ProblemBody.Received.class)))
    })
    public ResponseEntity<Void> receive(
            @PathVariable long id, @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        incidents.receive(id, currentUser);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/change-level")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Đã đổi mức"),
        @ApiResponse(
                responseCode = "403",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = M05ProblemBody.class))),
        @ApiResponse(
                responseCode = "422",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = M05ProblemBody.class)))
    })
    public ResponseEntity<Void> changeLevel(
            @PathVariable long id,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody ChangeIncidentLevelRequest request) {
        incidents.changeLevel(incidents.target(id), id, request.newLevel(), request.reason(), currentUser);
        return ResponseEntity.ok().build();
    }
}
