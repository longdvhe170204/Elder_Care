package com.eldercare.backend.incident.web;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.incident.internal.alert.service.AlertCommandService;
import com.eldercare.backend.incident.web.response.M05ProblemBody;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Lệnh xử lý cảnh báo (bảng FR-029; T031 một phần: Tiếp nhận). */
@RestController
@RequestMapping("/api/v1/alerts")
public class AlertController {
    private static final String PROBLEM = "application/problem+json";

    private final AlertCommandService alerts;
    private final CurrentUser currentUser;

    public AlertController(AlertCommandService alerts, CurrentUser currentUser) {
        this.alerts = alerts;
        this.currentUser = currentUser;
    }

    @PostMapping("/{id}/receive")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(
                responseCode = "403",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = M05ProblemBody.class))),
        @ApiResponse(
                responseCode = "409",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = M05ProblemBody.class)))
    })
    public ResponseEntity<Void> receive(
            @PathVariable long id, @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        alerts.receive(id, currentUser);
        return ResponseEntity.ok().build();
    }
}
