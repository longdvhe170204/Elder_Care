package com.eldercare.backend.incident.web;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.incident.internal.alert.domain.AlertDetails;
import com.eldercare.backend.incident.internal.alert.domain.AlertStatus;
import com.eldercare.backend.incident.internal.alert.service.AlertCommandService;
import com.eldercare.backend.incident.internal.alert.service.AlertHandlingService;
import com.eldercare.backend.incident.internal.alert.service.AlertQueryService;
import com.eldercare.backend.incident.internal.alert.service.ManualAlertService;
import com.eldercare.backend.incident.web.request.AssignAlertRequest;
import com.eldercare.backend.incident.web.request.ChangeAlertLevelRequest;
import com.eldercare.backend.incident.web.request.CloseAlertRequest;
import com.eldercare.backend.incident.web.request.CreateAlertRequest;
import com.eldercare.backend.incident.web.response.AlertResponse;
import com.eldercare.backend.incident.web.response.M05ProblemBody;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.util.List;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Cảnh báo (bảng FR-029; T031): danh sách, chi tiết, tạo thủ công, Tiếp nhận, Giao, Bắt đầu xử lý, Đổi mức, Đóng. */
@RestController
@RequestMapping("/api/v1/alerts")
public class AlertController {
    private static final String PROBLEM = "application/problem+json";

    private final AlertCommandService alerts;
    private final AlertHandlingService handling;
    private final ManualAlertService manual;
    private final AlertQueryService queries;
    private final CurrentUser currentUser;

    public AlertController(
            AlertCommandService alerts,
            AlertHandlingService handling,
            ManualAlertService manual,
            AlertQueryService queries,
            CurrentUser currentUser) {
        this.alerts = alerts;
        this.handling = handling;
        this.manual = manual;
        this.queries = queries;
        this.currentUser = currentUser;
    }

    @GetMapping
    @ApiResponses({@ApiResponse(responseCode = "200", description = "OK")})
    public List<AlertResponse> list(
            @RequestParam(required = false) List<AlertStatus> status,
            @RequestParam(required = false) Long floorAreaId,
            @RequestParam(required = false) Long residentId,
            @RequestParam(required = false) Boolean maxEscalationReached) {
        return queries.list(status, floorAreaId, residentId, maxEscalationReached, currentUser).stream()
                .map(details -> AlertResponse.of(details, false))
                .toList();
    }

    @PostMapping
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Tạo hoặc gộp"),
        @ApiResponse(
                responseCode = "403",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = M05ProblemBody.class))),
        @ApiResponse(
                responseCode = "409",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = M05ProblemBody.class)))
    })
    public ResponseEntity<AlertResponse> create(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody CreateAlertRequest request) {
        var created = manual.create(
                request.residentId(), request.incidentTypeId(), request.level(), request.description(), currentUser);
        var details = new AlertDetails(created.alert(), null, List.of(), Set.of());
        return ResponseEntity.status(HttpStatus.CREATED).body(AlertResponse.of(details, created.merged()));
    }

    @GetMapping("/{id}")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "OK")})
    public AlertResponse detail(@PathVariable long id) {
        return AlertResponse.of(queries.detail(id, currentUser), false);
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

    @PostMapping("/{id}/assign")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(
                responseCode = "403",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = M05ProblemBody.class))),
        @ApiResponse(
                responseCode = "409",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = M05ProblemBody.class)))
    })
    public ResponseEntity<Void> assign(
            @PathVariable long id,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody AssignAlertRequest request) {
        handling.assign(id, request.staffId(), request.note(), currentUser);
        return ResponseEntity.ok().build();
    }

    /** Mốc Đã tới nơi của cảnh báo nút SOS, nút hỗ trợ (FR-047j, BR-M05-30). */
    @PostMapping("/{id}/mark-arrived")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(
                responseCode = "403",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = M05ProblemBody.class))),
        @ApiResponse(
                responseCode = "409",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = M05ProblemBody.class)))
    })
    public ResponseEntity<Void> markArrived(
            @PathVariable long id, @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        handling.markArrived(id, currentUser);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/start-handling")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(
                responseCode = "403",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = M05ProblemBody.class))),
        @ApiResponse(
                responseCode = "409",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = M05ProblemBody.class)))
    })
    public ResponseEntity<Void> startHandling(
            @PathVariable long id, @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        handling.startHandling(id, currentUser);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/change-level")
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
    public ResponseEntity<Void> changeLevel(
            @PathVariable long id,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody ChangeAlertLevelRequest request) {
        handling.changeLevel(handling.target(id), id, request.newLevel(), request.reason(), currentUser);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/close")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(
                responseCode = "409",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = M05ProblemBody.class))),
        @ApiResponse(
                responseCode = "422",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = M05ProblemBody.class)))
    })
    public ResponseEntity<Void> close(
            @PathVariable long id,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody CloseAlertRequest request) {
        handling.close(id, request.resolution(), request.reason(), currentUser);
        return ResponseEntity.ok().build();
    }
}
