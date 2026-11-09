package com.eldercare.backend.health.web;

import com.eldercare.backend.administration.account.api.AccountApi;
import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.RequiresPermission;
import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.administration.correction.api.CorrectionApi;
import com.eldercare.backend.administration.correction.api.dto.CurrentValue;
import com.eldercare.backend.health.internal.measurement.domain.VitalSignRecord;
import com.eldercare.backend.health.internal.measurement.service.VitalSignCorrectionProvider;
import com.eldercare.backend.health.internal.measurement.service.VitalSignMeasurementService;
import com.eldercare.backend.health.web.request.RecordVitalSignRequest;
import com.eldercare.backend.health.web.request.RemeasureVitalSignRequest;
import com.eldercare.backend.health.web.response.M06ProblemBody;
import com.eldercare.backend.health.web.response.VitalSignRecordResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller xử lý đo và tra cứu chỉ số sinh hiệu (FR-010 -> FR-018, UC-32).
 */
@RestController
@RequestMapping("/api/v1")
public class VitalSignController {

    private static final String PROBLEM = "application/problem+json";

    private final VitalSignMeasurementService measurementService;
    private final CurrentUser currentUser;
    private final AccountApi accounts;
    private final CorrectionApi corrections;
    private final VitalSignCorrectionProvider correctionProvider;

    public VitalSignController(
            VitalSignMeasurementService measurementService,
            CurrentUser currentUser,
            AccountApi accounts,
            CorrectionApi corrections,
            VitalSignCorrectionProvider correctionProvider) {
        this.measurementService = measurementService;
        this.currentUser = currentUser;
        this.accounts = accounts;
        this.corrections = corrections;
        this.correctionProvider = correctionProvider;
    }

    @GetMapping("/residents/{residentId}/vital-signs")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(
                responseCode = "403",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = M06ProblemBody.class)))
    })
    @RequiresPermission(function = Function.VITAL_SIGN, operation = Operation.VIEW)
    public List<VitalSignRecordResponse> getVitalSigns(
            @PathVariable("residentId") long residentId,
            @RequestParam(value = "indicatorId", required = false) Long indicatorId,
            @RequestParam(value = "from", required = false) Instant from,
            @RequestParam(value = "to", required = false) Instant to) {
        List<VitalSignRecord> records = measurementService.getHistory(residentId, indicatorId, from, to);
        var current = corrections.current(
                VitalSignCorrectionProvider.RECORD_TYPE,
                records.stream().map(VitalSignRecord::id).toList());
        return records.stream()
                .filter(record -> current.get(record.id()) == null
                        || !current.get(record.id()).voided())
                .map(record -> corrected(record, current.get(record.id())))
                .toList();
    }

    @PostMapping("/residents/{residentId}/vital-signs")
    @ApiResponses({
        @ApiResponse(
                responseCode = "201",
                content =
                        @Content(
                                mediaType = "application/json",
                                schema = @Schema(implementation = VitalSignRecordResponse.class))),
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
    @RequiresPermission(function = Function.VITAL_SIGN, operation = Operation.CREATE)
    public ResponseEntity<VitalSignRecordResponse> recordVitalSign(
            @PathVariable("residentId") long residentId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody RecordVitalSignRequest request) {
        var command = request.toCommand(residentId, currentUser.accountId());
        var record = measurementService.record(command, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(response(record));
    }

    @PostMapping("/vital-sign-records/{id}/remeasure")
    @ApiResponses({
        @ApiResponse(
                responseCode = "201",
                content =
                        @Content(
                                mediaType = "application/json",
                                schema = @Schema(implementation = VitalSignRecordResponse.class))),
        @ApiResponse(
                responseCode = "409",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = M06ProblemBody.class))),
        @ApiResponse(
                responseCode = "422",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = M06ProblemBody.class)))
    })
    @RequiresPermission(function = Function.VITAL_SIGN, operation = Operation.CREATE)
    public ResponseEntity<VitalSignRecordResponse> remeasureVitalSign(
            @PathVariable("id") long draftId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody RemeasureVitalSignRequest request) {
        var command = request.toCommand(draftId, currentUser.accountId());
        var record = measurementService.remeasure(command, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(response(record));
    }

    /** Lịch sử dùng giá trị hiện hành sau đính chính, kèm giá trị gốc và mức tính lại (BR-M06-06). */
    private VitalSignRecordResponse corrected(VitalSignRecord record, CurrentValue current) {
        var base = response(record);
        if (current == null || current.content() == null) return base;
        var values = VitalSignCorrectionProvider.values(current.content());
        return base.corrected(values, correctionProvider.levelOf(record, values));
    }

    private VitalSignRecordResponse response(VitalSignRecord record) {
        String name = accounts.fullName(record.measuredBy()).orElse("#" + record.measuredBy());
        return VitalSignRecordResponse.from(record, name);
    }
}
