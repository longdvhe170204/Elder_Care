package com.cambae.backend.m07.web;

import com.cambae.backend.common.Role;
import com.cambae.backend.common.error.web.ProblemBody;
import com.cambae.backend.common.web.VersionEtag;
import com.cambae.backend.m07.internal.service.MedicationReconciliationService;
import com.cambae.backend.m07.internal.service.request.CreateReconciliationBody;
import com.cambae.backend.m07.internal.service.request.ProposeCorrectionBody;
import com.cambae.backend.m07.internal.service.response.ReconciliationResponse;
import com.cambae.backend.m07.web.request.ReasonRequest;
import com.cambae.backend.m07.web.request.ReconciliationRequests.NewReconciliationRequest;
import com.cambae.backend.m07.web.request.ReconciliationRequests.ProposeCorrectionRequest;
import com.cambae.backend.m07.web.request.ReconciliationRequests.ReconciliationItemRequest;
import com.cambae.backend.m07.web.response.ReconciliationBodies.ReconciliationBody;
import com.cambae.backend.m15.account.api.CurrentUser;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/** Đối chiếu thuốc theo api-006 (thẻ {@code reconciliation}; FR-034 → FR-039). */
@RestController
@Tag(name = "reconciliation")
public class MedicationReconciliationController {

    private static final String PROBLEM = "application/problem+json";

    private final MedicationReconciliationService reconciliationService;
    private final CurrentUser currentUser;

    public MedicationReconciliationController(
            MedicationReconciliationService reconciliationService, CurrentUser currentUser) {
        this.reconciliationService = reconciliationService;
        this.currentUser = currentUser;
    }

    private boolean isDoctor() {
        return currentUser.roles().contains(Role.DOCTOR);
    }

    @GetMapping("/api/v1/residents/{residentId}/medication-reconciliations")
    @ApiResponse(responseCode = "200", description = "OK")
    public List<ReconciliationBody> listByResident(@PathVariable("residentId") Long residentId) {
        return reconciliationService.listByResident(residentId).stream()
                .map(ReconciliationBody::of)
                .toList();
    }

    @PostMapping("/api/v1/residents/{residentId}/medication-reconciliations")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Mở", content = @Content),
        @ApiResponse(
                responseCode = "409",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class))),
        @ApiResponse(
                responseCode = "422",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class)))
    })
    public ResponseEntity<Void> create(
            @PathVariable("residentId") Long residentId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody NewReconciliationRequest body) {
        CreateReconciliationBody command = new CreateReconciliationBody();
        command.setReason(body.reason().name());
        command.setReasonDetail(body.reasonDetail());
        ReconciliationResponse created =
                reconciliationService.create(residentId, command, currentUser.accountId(), isDoctor());
        return ResponseEntity.status(HttpStatus.CREATED)
                .eTag(VersionEtag.of(created.getVersion()))
                .build();
    }

    @GetMapping("/api/v1/medication-reconciliations/{id}")
    @ApiResponse(responseCode = "200", description = "OK")
    public ResponseEntity<ReconciliationBody> getById(@PathVariable("id") Long id) {
        ReconciliationResponse res = reconciliationService.get(id);
        return ResponseEntity.ok().eTag(VersionEtag.of(res.getVersion())).body(ReconciliationBody.of(res));
    }

    @PutMapping("/api/v1/medication-reconciliations/{id}/items")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK", content = @Content),
        @ApiResponse(
                responseCode = "409",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class))),
        @ApiResponse(
                responseCode = "412",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class)))
    })
    public ResponseEntity<Void> updateItems(
            @PathVariable("id") Long id,
            @RequestHeader("If-Match") String ifMatch,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody List<@Valid ReconciliationItemRequest> items) {
        ReconciliationResponse res = reconciliationService.updateItems(
                id,
                ifMatch,
                items.stream().map(M07RequestMapper::reconciliationItem).toList(),
                currentUser.accountId());
        return ResponseEntity.ok().eTag(VersionEtag.of(res.getVersion())).build();
    }

    @PostMapping("/api/v1/medication-reconciliations/{id}/submit")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Chờ xác nhận", content = @Content),
        @ApiResponse(
                responseCode = "409",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class)))
    })
    public ResponseEntity<Void> submit(
            @PathVariable("id") Long id,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        ReconciliationResponse res = reconciliationService.submit(id, currentUser.accountId());
        return ResponseEntity.ok().eTag(VersionEtag.of(res.getVersion())).build();
    }

    @PostMapping("/api/v1/medication-reconciliations/{id}/return")
    @ApiResponse(responseCode = "200", description = "Mở", content = @Content)
    public ResponseEntity<Void> returnTicket(
            @PathVariable("id") Long id,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody ReasonRequest body) {
        ReconciliationResponse res = reconciliationService.returnForEdit(id, body.reason(), currentUser.accountId());
        return ResponseEntity.ok().eTag(VersionEtag.of(res.getVersion())).build();
    }

    @PostMapping("/api/v1/medication-reconciliations/{id}/confirm")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Đã xác nhận", content = @Content),
        @ApiResponse(
                responseCode = "403",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class))),
        @ApiResponse(
                responseCode = "409",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class)))
    })
    public ResponseEntity<Void> confirm(
            @PathVariable("id") Long id,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        ReconciliationResponse res = reconciliationService.confirm(id, currentUser.accountId(), isDoctor());
        return ResponseEntity.ok().eTag(VersionEtag.of(res.getVersion())).build();
    }

    @PostMapping("/api/v1/medication-reconciliations/{id}/propose-correction")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Chờ xác nhận", content = @Content),
        @ApiResponse(
                responseCode = "403",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class)))
    })
    public ResponseEntity<Void> proposeCorrection(
            @PathVariable("id") Long id,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody ProposeCorrectionRequest body) {
        ProposeCorrectionBody command = new ProposeCorrectionBody();
        command.setCorrectedContent(body.correctedContent());
        command.setReason(body.reason());
        reconciliationService.proposeCorrection(id, command, currentUser.accountId());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/api/v1/reconciliation-corrections/{id}/confirm")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK", content = @Content),
        @ApiResponse(
                responseCode = "409",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class)))
    })
    public ResponseEntity<Void> confirmCorrection(
            @PathVariable("id") Long id,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        reconciliationService.confirmCorrection(id, currentUser.accountId());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/api/v1/reconciliation-corrections/{id}/return")
    @ApiResponse(responseCode = "200", description = "OK", content = @Content)
    public ResponseEntity<Void> rejectCorrection(
            @PathVariable("id") Long id,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody ReasonRequest body) {
        reconciliationService.rejectCorrection(id, body.reason(), currentUser.accountId());
        return ResponseEntity.ok().build();
    }
}
