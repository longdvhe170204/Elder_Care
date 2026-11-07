package com.eldercare.backend.incident.web;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.incident.internal.incident.service.IncidentProgressionService;
import com.eldercare.backend.incident.web.request.AddProgressionRequest;
import com.eldercare.backend.incident.web.response.M05ProblemBody;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Lệnh ghi diễn biến sự cố (bảng FR-045, FR-048a). */
@RestController
@RequestMapping("/api/v1/incidents")
public class IncidentProgressionController {
    private static final String PROBLEM = "application/problem+json";

    private final IncidentProgressionService progressions;
    private final CurrentUser currentUser;

    public IncidentProgressionController(IncidentProgressionService progressions, CurrentUser currentUser) {
        this.progressions = progressions;
        this.currentUser = currentUser;
    }

    @PostMapping("/{id}/progressions")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "OK"),
        @ApiResponse(
                responseCode = "403",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = M05ProblemBody.class))),
        @ApiResponse(
                responseCode = "409",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = M05ProblemBody.class)))
    })
    public ResponseEntity<Void> add(
            @PathVariable long id,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody AddProgressionRequest request) {
        progressions.add(
                id,
                request.progressionTypeId() == null ? 0 : request.progressionTypeId(),
                request.content(),
                request.occurredAt(),
                currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
