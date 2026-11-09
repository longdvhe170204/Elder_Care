package com.eldercare.backend.health.web;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.health.internal.threshold.service.ThresholdService;
import com.eldercare.backend.health.web.request.HealthReasonRequest;
import com.eldercare.backend.health.web.request.ThresholdRequest;
import com.eldercare.backend.health.web.response.M06ProblemBody;
import com.eldercare.backend.health.web.response.ThresholdVersionResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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

/** Ngưỡng cảnh báo (FR-020 → FR-025; T024): lịch sử, thiết lập, hủy, ngừng, danh sách chưa có ngưỡng cá nhân. */
@RestController
@RequestMapping("/api/v1")
public class ThresholdController {
    private static final String PROBLEM = "application/problem+json";

    private final ThresholdService thresholds;
    private final CurrentUser currentUser;

    public ThresholdController(ThresholdService thresholds, CurrentUser currentUser) {
        this.thresholds = thresholds;
        this.currentUser = currentUser;
    }

    @GetMapping("/thresholds")
    public List<ThresholdVersionResponse> list(
            @RequestParam(required = false) @Schema(allowableValues = {"FACILITY", "PERSONAL"}) String scope,
            @RequestParam(required = false) Long residentId,
            @RequestParam(required = false) Long indicatorId) {
        return thresholds.list(scope, residentId, indicatorId).stream()
                .map(ThresholdVersionResponse::from)
                .toList();
    }

    @PostMapping("/thresholds")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Đã thiết lập"),
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
    public ResponseEntity<Void> set(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody ThresholdRequest request) {
        thresholds.set(request.toDomain(), currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/thresholds/{id}/cancel")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(
                responseCode = "409",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = M06ProblemBody.class)))
    })
    public ResponseEntity<Void> cancel(
            @PathVariable long id,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody HealthReasonRequest request) {
        thresholds.cancel(id, request.reason(), currentUser);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/thresholds/{id}/stop")
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
        thresholds.stop(id, request.reason(), currentUser);
        return ResponseEntity.ok().build();
    }

    /** FR-025: mảng mã người cao tuổi (api-007, chủ dự án duyệt schema 2026-11-07). */
    @GetMapping("/vital-signs/missing-personal-threshold")
    public List<Long> missingPersonalThreshold() {
        return thresholds.missingPersonal();
    }
}
