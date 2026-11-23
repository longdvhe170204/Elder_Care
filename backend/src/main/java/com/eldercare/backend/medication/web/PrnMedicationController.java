package com.eldercare.backend.medication.web;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.common.error.web.ProblemBody;
import com.eldercare.backend.medication.internal.dose.domain.RecordPrnBody;
import com.eldercare.backend.medication.internal.dose.service.PrnMedicationService;
import com.eldercare.backend.medication.internal.governance.service.ActorNames;
import com.eldercare.backend.medication.web.request.DoseRequests.ResultBasis;
import com.eldercare.backend.medication.web.request.NewPrnAdministrationRequest;
import com.eldercare.backend.medication.web.response.M07Problems;
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
    private final com.eldercare.backend.medication.internal.dose.service.PrnEffectivenessService effectiveness;
    private final ActorNames names;
    private final CurrentUser currentUser;

    public PrnMedicationController(
            PrnMedicationService prnService,
            com.eldercare.backend.medication.internal.dose.service.PrnEffectivenessService effectiveness,
            ActorNames names,
            CurrentUser currentUser) {
        this.prnService = prnService;
        this.effectiveness = effectiveness;
        this.names = names;
        this.currentUser = currentUser;
    }

    @GetMapping("/api/v1/prescriptions/{id}/prn-administrations")
    @ApiResponse(responseCode = "200", description = "OK")
    public PrnHistoryBody getPrnHistory(@PathVariable("id") Long prescriptionId) {
        var history = prnService.getPrnHistory(prescriptionId);
        var recorded = effectiveness.byAdministration(
                history.getAdministrations().stream().map(a -> a.getId()).toList());
        return PrnHistoryBody.of(history, names::of, recorded, effectiveness.indicationOf(prescriptionId));
    }

    /** api-006 {@code PrnEffectivenessRequest}. */
    public record PrnEffectivenessRequest(
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
            @jakarta.validation.constraints.NotBlank
            @jakarta.validation.constraints.Size(max = 2000)
            String effectiveness) {}

    @PostMapping("/api/v1/prescriptions/{id}/prn-administrations/{prnId}/effectiveness")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Đã ghi hiệu quả", content = @Content),
        @ApiResponse(
                responseCode = "404",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class))),
        @ApiResponse(
                responseCode = "409",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class)))
    })
    public ResponseEntity<Void> recordEffectiveness(
            @PathVariable("id") Long prescriptionId,
            @PathVariable("prnId") Long prnId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody PrnEffectivenessRequest body) {
        effectiveness.record(prescriptionId, prnId, body.effectiveness(), currentUser.accountId());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/api/v1/prescriptions/{id}/prn-administrations")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Đã dùng", content = @Content),
        @ApiResponse(
                responseCode = "409",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = M07Problems.Prn.class))),
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
