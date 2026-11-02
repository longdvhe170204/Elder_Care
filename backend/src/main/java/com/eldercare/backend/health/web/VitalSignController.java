package com.eldercare.backend.health.web;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.RequiresPermission;
import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.health.internal.measurement.domain.VitalSignRecord;
import com.eldercare.backend.health.internal.measurement.service.VitalSignMeasurementService;
import com.eldercare.backend.health.web.request.RecordVitalSignRequest;
import com.eldercare.backend.health.web.request.RemeasureVitalSignRequest;
import com.eldercare.backend.health.web.response.VitalSignRecordResponse;
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

    private final VitalSignMeasurementService measurementService;
    private final CurrentUser currentUser;

    public VitalSignController(VitalSignMeasurementService measurementService, CurrentUser currentUser) {
        this.measurementService = measurementService;
        this.currentUser = currentUser;
    }

    @GetMapping({"/nguoi-cao-tuoi/{nctId}/chi-so", "/residents/{nctId}/vital-signs"})
    @RequiresPermission(function = Function.VITAL_SIGN, operation = Operation.VIEW)
    public List<VitalSignRecordResponse> getVitalSigns(
            @PathVariable("nctId") long residentId,
            @RequestParam(value = "chiSoId", required = false) Long indicatorId,
            @RequestParam(value = "tu", required = false) Instant from,
            @RequestParam(value = "den", required = false) Instant to) {
        List<VitalSignRecord> records = measurementService.getHistory(residentId, indicatorId, from, to);
        return records.stream().map(VitalSignRecordResponse::from).toList();
    }

    @PostMapping({"/nguoi-cao-tuoi/{nctId}/chi-so", "/residents/{nctId}/vital-signs"})
    @RequiresPermission(function = Function.VITAL_SIGN, operation = Operation.CREATE)
    public ResponseEntity<VitalSignRecordResponse> recordVitalSign(
            @PathVariable("nctId") long residentId, @RequestBody RecordVitalSignRequest request) {
        var command = request.toCommand(residentId, currentUser.accountId());
        var record = measurementService.record(command, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(VitalSignRecordResponse.from(record));
    }

    @PostMapping({"/ban-ghi-chi-so/{id}/do-lai", "/vital-sign-records/{id}/remeasure"})
    @RequiresPermission(function = Function.VITAL_SIGN, operation = Operation.CREATE)
    public ResponseEntity<VitalSignRecordResponse> remeasureVitalSign(
            @PathVariable("id") long draftId, @RequestBody RemeasureVitalSignRequest request) {
        var command = request.toCommand(draftId, currentUser.accountId());
        var record = measurementService.remeasure(command, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(VitalSignRecordResponse.from(record));
    }
}
