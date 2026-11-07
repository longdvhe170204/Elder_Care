package com.eldercare.backend.care.web;

import com.eldercare.backend.care.internal.qualitycheck.service.QualityCheckService;
import com.eldercare.backend.care.web.request.QualityCheckResultWrite;
import com.eldercare.backend.care.web.request.QualityCheckSelectionWrite;
import com.eldercare.backend.care.web.response.CareProblemBody;
import com.eldercare.backend.care.web.response.QualityCheckShiftResponse;
import com.eldercare.backend.common.error.InvalidValueException;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** 014 T025 (FR-059, FR-061 → FR-064): xem, lập danh sách, ghi kết quả (schema GET duyệt 07/11/2026). */
@RestController
@RequestMapping("/api/v1")
public class QualityCheckController {
    private final QualityCheckService checks;

    public QualityCheckController(QualityCheckService checks) {
        this.checks = checks;
    }

    @GetMapping("/quality-checks/shifts/{shiftId}")
    @ApiResponse(responseCode = "200", description = "OK")
    @ApiResponse(
            responseCode = "403",
            content =
                    @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = CareProblemBody.class)))
    public QualityCheckShiftResponse overview(@PathVariable long shiftId, @RequestParam long floorAreaId) {
        return QualityCheckShiftResponse.of(
                checks.overview(QualityCheckService.target(floorAreaId), shiftId, floorAreaId));
    }

    @PostMapping("/quality-checks/shifts/{shiftId}")
    @ApiResponse(responseCode = "201", description = "OK")
    @ApiResponse(
            responseCode = "403",
            content =
                    @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = CareProblemBody.class)))
    @ApiResponse(
            responseCode = "409",
            content =
                    @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = CareProblemBody.class)))
    public ResponseEntity<Void> select(
            @PathVariable long shiftId,
            @RequestHeader("Idempotency-Key") String key,
            @RequestBody QualityCheckSelectionWrite body) {
        if (body.floorAreaId() == null) throw new InvalidValueException("Cần tầng/khu vực của ca");
        checks.select(QualityCheckService.target(body.floorAreaId()), shiftId, body.floorAreaId(), body.careTaskIds());
        return ResponseEntity.created(
                        URI.create("/api/v1/quality-checks/shifts/" + shiftId + "?floorAreaId=" + body.floorAreaId()))
                .build();
    }

    @PostMapping("/quality-check-items/{id}/result")
    @ApiResponse(responseCode = "200", description = "OK")
    @ApiResponse(
            responseCode = "403",
            content =
                    @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = CareProblemBody.class)))
    @ApiResponse(
            responseCode = "409",
            content =
                    @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = CareProblemBody.class)))
    public void result(
            @PathVariable long id,
            @RequestHeader("Idempotency-Key") String key,
            @RequestBody QualityCheckResultWrite body) {
        var rework = body.reworkTask() == null
                ? null
                : new QualityCheckService.Rework(
                        body.reworkTask().plannedAt(),
                        body.reworkTask().performerId(),
                        Boolean.TRUE.equals(body.reworkTask().sharedOnFloor()));
        checks.record(checks.itemTarget(id), id, body.result(), body.note(), body.photoFileIds(), rework);
    }
}
