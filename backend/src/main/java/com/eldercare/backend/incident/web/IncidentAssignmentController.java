package com.eldercare.backend.incident.web;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.incident.internal.incident.service.IncidentAssignmentService;
import com.eldercare.backend.incident.web.request.AssignIncidentHandlerRequest;
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

/** Lệnh giao người xử lý sự cố (FR-045a; T037 một phần). */
@RestController
@RequestMapping("/api/v1/incidents")
public class IncidentAssignmentController {
    private static final String PROBLEM = "application/problem+json";

    private final IncidentAssignmentService assignments;
    private final CurrentUser currentUser;

    public IncidentAssignmentController(IncidentAssignmentService assignments, CurrentUser currentUser) {
        this.assignments = assignments;
        this.currentUser = currentUser;
    }

    @PostMapping("/{id}/assign-handler")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(
                responseCode = "403",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = M05ProblemBody.class))),
        @ApiResponse(
                responseCode = "409",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = M05ProblemBody.class))),
        @ApiResponse(
                responseCode = "422",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = M05ProblemBody.class)))
    })
    public ResponseEntity<Void> assign(
            @PathVariable long id,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody AssignIncidentHandlerRequest request) {
        assignments.assign(id, request.assigneeId() == null ? 0 : request.assigneeId(), request.note(), currentUser);
        return ResponseEntity.ok().build();
    }
}
