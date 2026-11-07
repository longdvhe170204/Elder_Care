package com.eldercare.backend.incident.web;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.incident.internal.incident.service.PostFallMonitoringService;
import com.eldercare.backend.incident.web.request.IncidentReasonRequest;
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

/** Lệnh kết thúc theo dõi sau ngã (FR-052). */
@RestController
@RequestMapping("/api/v1/incidents")
public class PostFallMonitoringController {
    private static final String PROBLEM = "application/problem+json";

    private final PostFallMonitoringService monitoring;
    private final CurrentUser currentUser;

    public PostFallMonitoringController(PostFallMonitoringService monitoring, CurrentUser currentUser) {
        this.monitoring = monitoring;
        this.currentUser = currentUser;
    }

    @PostMapping("/{id}/end-post-fall-monitoring")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(
                responseCode = "409",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = M05ProblemBody.class)))
    })
    public ResponseEntity<Void> end(
            @PathVariable long id,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody IncidentReasonRequest request) {
        monitoring.endEarly(id, request.reason(), currentUser);
        return ResponseEntity.ok().build();
    }
}
