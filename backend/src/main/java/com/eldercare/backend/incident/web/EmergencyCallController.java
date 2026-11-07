package com.eldercare.backend.incident.web;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.incident.internal.emergency.service.EmergencyCallService;
import com.eldercare.backend.incident.web.request.Call115ResultRequest;
import com.eldercare.backend.incident.web.response.Call115AttemptResponse;
import com.eldercare.backend.incident.web.response.Call115InfoResponse;
import com.eldercare.backend.incident.web.response.M05ProblemBody;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/** Nút Gọi 115 trên cảnh báo, sự cố Khẩn cấp (US13; FR-047k, FR-048b, BR-M05-31). */
@RestController
public class EmergencyCallController {
    private static final String PROBLEM = "application/problem+json";

    private final EmergencyCallService calls;
    private final CurrentUser currentUser;

    public EmergencyCallController(EmergencyCallService calls, CurrentUser currentUser) {
        this.calls = calls;
        this.currentUser = currentUser;
    }

    @GetMapping("/api/v1/alerts/{id}/call-115-info")
    public Call115InfoResponse alertInfo(@PathVariable long id) {
        return Call115InfoResponse.of(calls.alertInfo(id, currentUser));
    }

    @PostMapping("/api/v1/alerts/{id}/call-115")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Lượt gọi"),
        @ApiResponse(
                responseCode = "403",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = M05ProblemBody.class)))
    })
    public ResponseEntity<Call115AttemptResponse> pressOnAlert(
            @PathVariable long id, @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Call115AttemptResponse.of(calls.pressOnAlert(id, currentUser)));
    }

    @GetMapping("/api/v1/incidents/{id}/call-115-info")
    public Call115InfoResponse incidentInfo(@PathVariable long id) {
        return Call115InfoResponse.of(calls.incidentInfo(id, currentUser));
    }

    @PostMapping("/api/v1/incidents/{id}/call-115")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Lượt gọi"),
        @ApiResponse(
                responseCode = "403",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = M05ProblemBody.class)))
    })
    public ResponseEntity<Call115AttemptResponse> pressOnIncident(
            @PathVariable long id, @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Call115AttemptResponse.of(calls.pressOnIncident(id, currentUser)));
    }

    @PostMapping("/api/v1/call-115-attempts/{id}/result")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(
                responseCode = "409",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = M05ProblemBody.class)))
    })
    public ResponseEntity<Void> result(@PathVariable long id, @RequestBody Call115ResultRequest request) {
        calls.result(id, request == null ? null : request.result(), currentUser);
        return ResponseEntity.ok().build();
    }
}
