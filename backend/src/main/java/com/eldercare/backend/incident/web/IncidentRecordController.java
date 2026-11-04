package com.eldercare.backend.incident.web;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.incident.api.dto.RecordIncidentCommand;
import com.eldercare.backend.incident.internal.incident.service.IncidentRecordService;
import com.eldercare.backend.incident.web.request.RecordIncidentRequest;
import com.eldercare.backend.incident.web.response.ActivationResultResponse;
import com.eldercare.backend.incident.web.response.ActivationResultResponse.ActivationIncident;
import com.eldercare.backend.incident.web.response.M05ProblemBody;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Lệnh ghi nhận sự cố {@code POST /incidents} (FR-040 → FR-043; T037 một phần). */
@RestController
@RequestMapping("/api/v1/incidents")
public class IncidentRecordController {
    private static final String PROBLEM = "application/problem+json";

    private final IncidentRecordService incidents;
    private final CurrentUser currentUser;

    public IncidentRecordController(IncidentRecordService incidents, CurrentUser currentUser) {
        this.incidents = incidents;
        this.currentUser = currentUser;
    }

    @PostMapping
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Đã ghi nhận"),
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
    public ResponseEntity<ActivationResultResponse> record(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody RecordIncidentRequest request) {
        var result = incidents.record(
                new RecordIncidentCommand(
                        request.residentId(),
                        request.incidentTypeId() == null ? 0 : request.incidentTypeId(),
                        request.context(),
                        request.occurredAt(),
                        request.discoveredAt(),
                        request.floorAreaId(),
                        request.location(),
                        request.ongoingActivity(),
                        request.description(),
                        request.level() == null ? null : request.level().name(),
                        request.lowerLevelReason(),
                        request.initialResponse(),
                        Boolean.TRUE.equals(request.createDespiteOpen())),
                currentUser);
        // Ngoài phạm vi chỉ trả nhận dạng và "đã thông báo người xử lý" (FR-041, FR-048).
        var incident = result.identificationOnly()
                ? new ActivationIncident(
                        result.incidentId(),
                        null,
                        null,
                        "NEW",
                        null,
                        null,
                        null,
                        null,
                        null,
                        true,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null)
                : new ActivationIncident(
                        result.incidentId(),
                        request,
                        request.incidentTypeId(),
                        "NEW",
                        request.level(),
                        request.level() == com.eldercare.backend.incident.internal.alert.domain.IncidentLevel.CRITICAL,
                        0,
                        null,
                        null,
                        false,
                        result.noOnDutyDoctor(),
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ActivationResultResponse(incident, null, result.identificationOnly()));
    }
}
