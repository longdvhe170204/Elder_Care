package com.eldercare.backend.medication.web;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.PermissionApi;
import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.administration.account.api.dto.PermissionKey;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.error.PermissionDeniedException;
import com.eldercare.backend.common.error.web.ProblemBody;
import com.eldercare.backend.common.web.VersionEtag;
import com.eldercare.backend.medication.internal.reconciliation.domain.CreateReconciliationBody;
import com.eldercare.backend.medication.internal.reconciliation.domain.ProposeCorrectionBody;
import com.eldercare.backend.medication.internal.reconciliation.domain.ReconciliationResponse;
import com.eldercare.backend.medication.internal.reconciliation.service.MedicationReconciliationService;
import com.eldercare.backend.medication.internal.reconciliation.service.ReconciliationWorklist;
import com.eldercare.backend.medication.web.request.ReasonRequest;
import com.eldercare.backend.medication.web.request.ReconciliationRequests.NewReconciliationRequest;
import com.eldercare.backend.medication.web.request.ReconciliationRequests.ProposeCorrectionRequest;
import com.eldercare.backend.medication.web.request.ReconciliationRequests.ReconciliationItemRequest;
import com.eldercare.backend.medication.web.response.M07Problems;
import com.eldercare.backend.medication.web.response.ReconciliationBodies.ReconciliationBody;
import com.eldercare.backend.medication.web.response.ReconciliationBodies.ReconciliationCorrectionBody;
import com.eldercare.backend.medication.web.response.ReconciliationBodies.ReconciliationWorklistEntryBody;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
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
    private final ReconciliationWorklist worklist;
    private final PermissionApi permissions;

    public MedicationReconciliationController(
            MedicationReconciliationService reconciliationService,
            CurrentUser currentUser,
            ReconciliationWorklist worklist,
            PermissionApi permissions) {
        this.reconciliationService = reconciliationService;
        this.currentUser = currentUser;
        this.worklist = worklist;
        this.permissions = permissions;
    }

    /**
     * Lệnh ghi phiếu đối chiếu chỉ cho người có quyền tương ứng ở Phụ lục 27 dòng "Đối chiếu thuốc, thuốc gia đình gửi"
     * (Bác sĩ, Điều dưỡng): CREATE lập phiếu; UPDATE lưu dòng, gửi, hủy, đề nghị đính chính; CONFIRM trả lại, xác nhận.
     */
    private void requirePermission(Operation operation, String message) {
        if (!permissions
                .permissions(currentUser)
                .contains(new PermissionKey(Function.MEDICATION_RECONCILIATION, operation))) {
            throw new PermissionDeniedException("PERMISSION_DENIED", message, Map.of("failedLayer", "ROLE"));
        }
    }

    private boolean isDoctor() {
        return currentUser.roles().contains(Role.DOCTOR);
    }

    @GetMapping("/api/v1/residents/{residentId}/medication-reconciliations")
    @ApiResponse(responseCode = "200", description = "OK")
    public List<ReconciliationBody> listByResident(@PathVariable("residentId") Long residentId) {
        return reconciliationService.listByResident(residentId).stream()
                .map(r -> ReconciliationBody.of(r, currentUser.accountId()))
                .toList();
    }

    @GetMapping("/api/v1/medication-reconciliation-worklist")
    @ApiResponse(responseCode = "200", description = "OK")
    public List<ReconciliationWorklistEntryBody> worklist() {
        return worklist.list().stream().map(ReconciliationWorklistEntryBody::of).toList();
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
        requirePermission(Operation.CREATE, "Chỉ Bác sĩ, Điều dưỡng lập được phiếu đối chiếu");
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
        return ResponseEntity.ok()
                .eTag(VersionEtag.of(res.getVersion()))
                .body(ReconciliationBody.of(res, currentUser.accountId()));
    }

    @PutMapping("/api/v1/medication-reconciliations/{id}/items")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK", content = @Content),
        @ApiResponse(
                responseCode = "409",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = M07Problems.Issues.class))),
        @ApiResponse(
                responseCode = "412",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class)))
    })
    public ResponseEntity<Void> updateItems(
            @PathVariable("id") Long id,
            @RequestHeader("If-Match") String ifMatch,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody List<@Valid ReconciliationItemRequest> items) {
        requirePermission(Operation.UPDATE, "Chỉ Bác sĩ, Điều dưỡng sửa được phiếu đối chiếu");
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
        requirePermission(Operation.UPDATE, "Chỉ Bác sĩ, Điều dưỡng gửi được phiếu đối chiếu");
        ReconciliationResponse res = reconciliationService.submit(id, currentUser.accountId());
        return ResponseEntity.ok().eTag(VersionEtag.of(res.getVersion())).build();
    }

    @PostMapping("/api/v1/medication-reconciliations/{id}/return")
    @ApiResponse(responseCode = "200", description = "Mở", content = @Content)
    public ResponseEntity<Void> returnTicket(
            @PathVariable("id") Long id,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody ReasonRequest body) {
        requirePermission(Operation.CONFIRM, "Chỉ Bác sĩ, Điều dưỡng trả lại được phiếu đối chiếu");
        ReconciliationResponse res = reconciliationService.returnForEdit(id, body.reason(), currentUser.accountId());
        return ResponseEntity.ok().eTag(VersionEtag.of(res.getVersion())).build();
    }

    /** Hủy phiếu Mở, Chờ xác nhận (Q-447): người có quyền ghi phiếu đối chiếu (Phụ lục 27: Bác sĩ, Điều dưỡng). */
    @PostMapping("/api/v1/medication-reconciliations/{id}/cancel")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Đã hủy", content = @Content),
        @ApiResponse(
                responseCode = "403",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class))),
        @ApiResponse(
                responseCode = "409",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class)))
    })
    public ResponseEntity<Void> cancel(
            @PathVariable("id") Long id,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody ReasonRequest body) {
        requirePermission(Operation.UPDATE, "Chỉ Bác sĩ, Điều dưỡng hủy được phiếu đối chiếu");
        ReconciliationResponse res = reconciliationService.cancel(id, body.reason());
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
        requirePermission(Operation.CONFIRM, "Chỉ Bác sĩ, Điều dưỡng xác nhận được phiếu đối chiếu");
        ReconciliationResponse res = reconciliationService.confirm(id, currentUser.accountId(), isDoctor());
        return ResponseEntity.ok().eTag(VersionEtag.of(res.getVersion())).build();
    }

    @PostMapping("/api/v1/medication-reconciliations/{id}/propose-correction")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Chờ xác nhận"),
        @ApiResponse(
                responseCode = "403",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class)))
    })
    public ResponseEntity<ReconciliationCorrectionBody> proposeCorrection(
            @PathVariable("id") Long id,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody ProposeCorrectionRequest body) {
        requirePermission(Operation.UPDATE, "Chỉ Bác sĩ, Điều dưỡng đề nghị đính chính được phiếu đối chiếu");
        ProposeCorrectionBody command = new ProposeCorrectionBody();
        command.setCorrectedContent(body.correctedContent());
        command.setReason(body.reason());
        var created = reconciliationService.proposeCorrection(id, command, currentUser.accountId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ReconciliationCorrectionBody.of(created, currentUser.accountId()));
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
        requirePermission(Operation.CONFIRM, "Chỉ Bác sĩ, Điều dưỡng xác nhận được đính chính phiếu đối chiếu");
        reconciliationService.confirmCorrection(id, currentUser.accountId());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/api/v1/reconciliation-corrections/{id}/return")
    @ApiResponse(responseCode = "200", description = "OK", content = @Content)
    public ResponseEntity<Void> rejectCorrection(
            @PathVariable("id") Long id,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody ReasonRequest body) {
        requirePermission(Operation.CONFIRM, "Chỉ Bác sĩ, Điều dưỡng trả lại được đính chính phiếu đối chiếu");
        reconciliationService.rejectCorrection(id, body.reason(), currentUser.accountId());
        return ResponseEntity.ok().build();
    }
}
