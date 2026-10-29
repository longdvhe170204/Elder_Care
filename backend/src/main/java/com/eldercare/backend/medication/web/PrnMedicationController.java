package com.eldercare.backend.medication.web;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.common.error.web.ProblemBody;
import com.eldercare.backend.medication.internal.service.ActorNames;
import com.eldercare.backend.medication.internal.service.PrnMedicationService;
import com.eldercare.backend.medication.internal.service.request.RecordPrnBody;
import com.eldercare.backend.medication.web.request.DoseRequests.ResultBasis;
import com.eldercare.backend.medication.web.request.NewPrnAdministrationRequest;
import com.eldercare.backend.medication.web.response.PrnBodies.PrnHistoryBody;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/** Thuốc dùng khi cần (PRN) theo api-006 (thẻ {@code prn}; FR-024, FR-025, BR-M07-04). */
@RestController
@Tag(name = "prn")
public class PrnMedicationController {

    private static final String PROBLEM = "application/problem+json";

    private final PrnMedicationService prnService;
    private final ActorNames names;
    private final CurrentUser currentUser;

    public PrnMedicationController(PrnMedicationService prnService, ActorNames names, CurrentUser currentUser) {
        this.prnService = prnService;
        this.names = names;
        this.currentUser = currentUser;
    }

    @GetMapping("/api/v1/prescriptions/{id}/prn-administrations")
    @ApiResponse(responseCode = "200", description = "OK")
    public PrnHistoryBody getPrnHistory(@PathVariable("id") Long prescriptionId) {
        return PrnHistoryBody.of(prnService.getPrnHistory(prescriptionId), names::of);
    }

    @PostMapping("/api/v1/prescriptions/{id}/prn-administrations")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Đã dùng", content = @Content),
        @ApiResponse(
                responseCode = "409",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class))),
        @ApiResponse(
                responseCode = "422",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class)))
    })
    public ResponseEntity<Void> recordPrn(
            @PathVariable("id") Long prescriptionId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody NewPrnAdministrationRequest body) {
        RecordPrnBody command = new RecordPrnBody();
        command.setAdministeredAt(body.administeredAt());
        command.setDosage(body.dosage());
        command.setReason(body.reason());
        command.setAdverseReaction(body.adverseReaction());
        command.setNote(body.note());
        // Ghi theo báo lại (FR-027): người báo lại ghi bằng họ tên
        if (body.basis() == ResultBasis.REPORTED_BY_OTHERS && body.reportedBy() != null) {
            command.setReportedBy(body.reportedBy().fullName());
        }
        command.setRecordedAfterDisconnect(Boolean.TRUE.equals(body.recordedAfterDisconnect()));
        prnService.recordPrn(prescriptionId, command, currentUser.accountId());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
