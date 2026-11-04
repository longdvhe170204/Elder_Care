package com.eldercare.backend.incident.web;

import com.eldercare.backend.common.error.InvalidValueException;
import com.eldercare.backend.incident.internal.infection.service.ContainmentService;
import com.eldercare.backend.incident.web.request.AttachIsolationIncidentRequest;
import com.eldercare.backend.incident.web.request.IsolationReasonRequest;
import com.eldercare.backend.incident.web.request.NewIsolationRequest;
import com.eldercare.backend.incident.web.response.IsolationResponse;
import com.eldercare.backend.incident.web.response.M05ProblemBody;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Lệnh HTTP khoanh vùng theo FR-063/064/066. */
@RestController
@RequestMapping("/api/v1/isolations")
class ContainmentController {
    private final ContainmentService service;

    ContainmentController(ContainmentService service) {
        this.service = service;
    }

    @PostMapping
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Đã khoanh vùng", content = @Content),
        @ApiResponse(
                responseCode = "403",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = M05ProblemBody.class))),
        @ApiResponse(
                responseCode = "409",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = M05ProblemBody.class))),
        @ApiResponse(
                responseCode = "422",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = M05ProblemBody.class)))
    })
    ResponseEntity<Void> create(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody NewIsolationRequest request) {
        service.create(request.toScope(), request.incidentIds(), request.reason());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping
    List<IsolationResponse> list() {
        return service.list().stream().map(IsolationResponse::from).toList();
    }

    @PostMapping("/{id}/lift")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Đã gỡ vùng", content = @Content),
        @ApiResponse(
                responseCode = "403",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = M05ProblemBody.class)))
    })
    ResponseEntity<Void> lift(
            @PathVariable long id,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody IsolationReasonRequest request) {
        service.lift(id, request.reason());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/attach-incident")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Đã gắn sự cố", content = @Content),
        @ApiResponse(
                responseCode = "409",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = M05ProblemBody.class)))
    })
    ResponseEntity<Void> attach(
            @PathVariable long id,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody AttachIsolationIncidentRequest request) {
        if (request.incidentId() == null) throw new InvalidValueException("Thiếu mã sự cố");
        service.attach(id, request.incidentId());
        return ResponseEntity.ok().build();
    }
}
