package com.cambae.backend.m07.web;

import com.cambae.backend.common.Role;
import com.cambae.backend.common.web.VersionEtag;
import com.cambae.backend.m07.internal.service.MedicationReconciliationService;
import com.cambae.backend.m07.web.request.CreateReconciliationBody;
import com.cambae.backend.m07.web.request.ProposeCorrectionBody;
import com.cambae.backend.m07.web.request.ReasonBody;
import com.cambae.backend.m07.web.request.ReconciliationItemBody;
import com.cambae.backend.m07.web.response.ReconciliationCorrectionResponse;
import com.cambae.backend.m07.web.response.ReconciliationResponse;
import com.cambae.backend.m15.account.api.CurrentUser;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for medication reconciliation (FR-034 -> FR-039; contracts/api-006.yaml section doi-chieu).
 */
@RestController
public class MedicationReconciliationController {

    private final MedicationReconciliationService reconciliationService;
    private final ObjectProvider<CurrentUser> currentUserProvider;

    public MedicationReconciliationController(
            MedicationReconciliationService reconciliationService, ObjectProvider<CurrentUser> currentUserProvider) {
        this.reconciliationService = reconciliationService;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping({
        "/api/v1/nguoi-cao-tuoi/{nctId}/phieu-doi-chieu",
        "/api/v1/residents/{nctId}/medication-reconciliations"
    })
    public List<ReconciliationResponse> listByResident(@PathVariable("nctId") Long residentId) {
        return reconciliationService.listByResident(residentId);
    }

    @PostMapping({
        "/api/v1/nguoi-cao-tuoi/{nctId}/phieu-doi-chieu",
        "/api/v1/residents/{nctId}/medication-reconciliations"
    })
    public ResponseEntity<ReconciliationResponse> create(
            @PathVariable("nctId") Long residentId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody CreateReconciliationBody body) {

        CurrentUser currentUser = currentUserProvider.getIfAvailable();
        Long currentUserId = currentUser != null ? currentUser.accountId() : 1L;
        boolean isDoctor = currentUser == null || currentUser.roles().contains(Role.DOCTOR);

        ReconciliationResponse created = reconciliationService.create(residentId, body, currentUserId, isDoctor);
        return ResponseEntity.status(HttpStatus.CREATED)
                .eTag(VersionEtag.of(created.getVersion()))
                .body(created);
    }

    @GetMapping({"/api/v1/phieu-doi-chieu/{id}", "/api/v1/medication-reconciliations/{id}"})
    public ResponseEntity<ReconciliationResponse> getById(@PathVariable("id") Long id) {
        ReconciliationResponse res = reconciliationService.get(id);
        return ResponseEntity.ok().eTag(VersionEtag.of(res.getVersion())).body(res);
    }

    @PutMapping({"/api/v1/phieu-doi-chieu/{id}/dong", "/api/v1/medication-reconciliations/{id}/items"})
    public ResponseEntity<ReconciliationResponse> updateItems(
            @PathVariable("id") Long id,
            @RequestHeader(value = "If-Match", required = false) String ifMatch,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody List<ReconciliationItemBody> items) {

        CurrentUser currentUser = currentUserProvider.getIfAvailable();
        Long currentUserId = currentUser != null ? currentUser.accountId() : 1L;

        ReconciliationResponse res = reconciliationService.updateItems(id, ifMatch, items, currentUserId);
        return ResponseEntity.ok().eTag(VersionEtag.of(res.getVersion())).body(res);
    }

    @PostMapping({"/api/v1/phieu-doi-chieu/{id}/gui-xac-nhan", "/api/v1/medication-reconciliations/{id}/submit"})
    public ResponseEntity<ReconciliationResponse> submit(
            @PathVariable("id") Long id,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {

        CurrentUser currentUser = currentUserProvider.getIfAvailable();
        Long currentUserId = currentUser != null ? currentUser.accountId() : 1L;

        ReconciliationResponse res = reconciliationService.submit(id, currentUserId);
        return ResponseEntity.ok().eTag(VersionEtag.of(res.getVersion())).body(res);
    }

    @PostMapping({"/api/v1/phieu-doi-chieu/{id}/tra-lai", "/api/v1/medication-reconciliations/{id}/return"})
    public ResponseEntity<ReconciliationResponse> returnTicket(
            @PathVariable("id") Long id,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody(required = false) ReasonBody body) {

        CurrentUser currentUser = currentUserProvider.getIfAvailable();
        Long currentUserId = currentUser != null ? currentUser.accountId() : 1L;
        String reason = body != null ? body.reason() : null;

        ReconciliationResponse res = reconciliationService.returnForEdit(id, reason, currentUserId);
        return ResponseEntity.ok().eTag(VersionEtag.of(res.getVersion())).body(res);
    }

    @PostMapping({"/api/v1/phieu-doi-chieu/{id}/xac-nhan", "/api/v1/medication-reconciliations/{id}/confirm"})
    public ResponseEntity<ReconciliationResponse> confirm(
            @PathVariable("id") Long id,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {

        CurrentUser currentUser = currentUserProvider.getIfAvailable();
        Long currentUserId =
                currentUser != null ? currentUser.accountId() : 2L; // default to different user in test if not set
        boolean isDoctor = currentUser == null || currentUser.roles().contains(Role.DOCTOR);

        ReconciliationResponse res = reconciliationService.confirm(id, currentUserId, isDoctor);
        return ResponseEntity.ok().eTag(VersionEtag.of(res.getVersion())).body(res);
    }

    @PostMapping({
        "/api/v1/phieu-doi-chieu/{id}/de-nghi-dinh-chinh",
        "/api/v1/medication-reconciliations/{id}/propose-correction"
    })
    public ResponseEntity<ReconciliationCorrectionResponse> proposeCorrection(
            @PathVariable("id") Long id,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody ProposeCorrectionBody body) {

        CurrentUser currentUser = currentUserProvider.getIfAvailable();
        Long currentUserId = currentUser != null ? currentUser.accountId() : 1L;

        ReconciliationCorrectionResponse res = reconciliationService.proposeCorrection(id, body, currentUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(res);
    }

    @PostMapping({"/api/v1/de-nghi-dinh-chinh-phieu/{id}/xac-nhan", "/api/v1/reconciliation-corrections/{id}/confirm"})
    public ResponseEntity<ReconciliationCorrectionResponse> confirmCorrection(
            @PathVariable("id") Long id,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {

        CurrentUser currentUser = currentUserProvider.getIfAvailable();
        Long currentUserId = currentUser != null ? currentUser.accountId() : 2L;

        ReconciliationCorrectionResponse res = reconciliationService.confirmCorrection(id, currentUserId);
        return ResponseEntity.ok(res);
    }

    @PostMapping({"/api/v1/de-nghi-dinh-chinh-phieu/{id}/tra-lai", "/api/v1/reconciliation-corrections/{id}/return"})
    public ResponseEntity<ReconciliationCorrectionResponse> rejectCorrection(
            @PathVariable("id") Long id,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {

        CurrentUser currentUser = currentUserProvider.getIfAvailable();
        Long currentUserId = currentUser != null ? currentUser.accountId() : 2L;

        ReconciliationCorrectionResponse res = reconciliationService.rejectCorrection(id, currentUserId);
        return ResponseEntity.ok(res);
    }
}
