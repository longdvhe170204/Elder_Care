package com.eldercare.backend.incident.web;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.incident.internal.incident.service.IncidentHandlingService;
import com.eldercare.backend.incident.web.request.CloseIncidentRequest;
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

/** Lệnh xử lý sự cố: tham gia, chuyển theo dõi, mở lại, đóng (bảng FR-045; T037 một phần). */
@RestController
@RequestMapping("/api/v1/incidents")
public class IncidentHandlingController {
    private static final String PROBLEM = "application/problem+json";

    private final IncidentHandlingService handling;
    private final CurrentUser currentUser;

    public IncidentHandlingController(IncidentHandlingService handling, CurrentUser currentUser) {
        this.handling = handling;
        this.currentUser = currentUser;
    }

    @PostMapping("/{id}/join-handling")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "OK")})
    public ResponseEntity<Void> join(
            @PathVariable long id, @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        handling.join(id, currentUser);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/move-to-monitoring")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(
                responseCode = "409",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = M05ProblemBody.class)))
    })
    public ResponseEntity<Void> moveToMonitoring(
            @PathVariable long id, @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        handling.moveToMonitoring(id, currentUser);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/reopen")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(
                responseCode = "422",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = M05ProblemBody.class)))
    })
    public ResponseEntity<Void> reopen(
            @PathVariable long id,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody IncidentReasonRequest request) {
        handling.reopen(id, request.reason(), currentUser);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/close")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(
                responseCode = "403",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = M05ProblemBody.class))),
        @ApiResponse(
                responseCode = "409",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = M05ProblemBody.class)))
    })
    public ResponseEntity<Void> close(
            @PathVariable long id,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody CloseIncidentRequest request) {
        handling.close(id, request.resolution(), request.infectionOutcome(), currentUser);
        return ResponseEntity.ok().build();
    }
}
