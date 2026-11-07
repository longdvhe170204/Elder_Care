package com.eldercare.backend.health.web;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.health.internal.schedule.service.MeasurementScheduleService;
import com.eldercare.backend.health.web.request.ChangeMeasurementScheduleRequest;
import com.eldercare.backend.health.web.request.HealthReasonRequest;
import com.eldercare.backend.health.web.request.MeasurementScheduleRequest;
import com.eldercare.backend.health.web.response.M06ProblemBody;
import com.eldercare.backend.health.web.response.MeasurementScheduleResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.time.Instant;
import java.util.List;
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

/** Lịch đo (FR-003 → FR-006; T024): danh sách, tạo, chuỗi mốc đo dự kiến, thay đổi, ngừng. */
@RestController
@RequestMapping("/api/v1")
public class MeasurementScheduleController {
    private static final String PROBLEM = "application/problem+json";

    private final MeasurementScheduleService schedules;
    private final CurrentUser currentUser;

    public MeasurementScheduleController(MeasurementScheduleService schedules, CurrentUser currentUser) {
        this.schedules = schedules;
        this.currentUser = currentUser;
    }

    @GetMapping("/residents/{residentId}/measurement-schedules")
    public List<MeasurementScheduleResponse> list(@PathVariable long residentId) {
        return schedules.list(new Target("M01.RESIDENT", Long.toString(residentId), residentId, null)).stream()
                .map(MeasurementScheduleResponse::from)
                .toList();
    }

    @PostMapping("/residents/{residentId}/measurement-schedules")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Đã tạo"),
        @ApiResponse(
                responseCode = "403",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = M06ProblemBody.class))),
        @ApiResponse(
                responseCode = "409",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = M06ProblemBody.class))),
        @ApiResponse(
                responseCode = "422",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = M06ProblemBody.class)))
    })
    public ResponseEntity<Void> create(
            @PathVariable long residentId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody MeasurementScheduleRequest request) {
        schedules.create(residentId, request.toDomain(), currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/measurement-schedules/{id}/measurement-times")
    public List<Instant> measurementTimes(@PathVariable long id, @RequestParam Instant from, @RequestParam Instant to) {
        return schedules.occurrences(id, from, to);
    }

    @PostMapping("/measurement-schedules/{id}/change")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Lịch mới"),
        @ApiResponse(
                responseCode = "409",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = M06ProblemBody.class))),
        @ApiResponse(
                responseCode = "422",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = M06ProblemBody.class)))
    })
    public ResponseEntity<Void> change(
            @PathVariable long id,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody ChangeMeasurementScheduleRequest request) {
        schedules.change(id, request.schedule().toDomain(), request.reason(), currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/measurement-schedules/{id}/stop")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(
                responseCode = "409",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = M06ProblemBody.class)))
    })
    public ResponseEntity<Void> stop(
            @PathVariable long id,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody HealthReasonRequest request) {
        schedules.stop(id, request.reason(), currentUser);
        return ResponseEntity.ok().build();
    }
}
