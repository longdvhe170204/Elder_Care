package com.cambae.backend.m07.web;

import com.cambae.backend.m07.internal.service.PrnMedicationService;
import com.cambae.backend.m07.web.request.RecordPrnBody;
import com.cambae.backend.m07.web.response.PrnAdministrationResponse;
import com.cambae.backend.m07.web.response.PrnHistoryResponse;
import com.cambae.backend.m15.account.api.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for as-needed (PRN) medication (FR-024, FR-025, BR-M07-04; contracts/api-006.yaml section prn).
 */
@RestController
public class PrnMedicationController {

    private final PrnMedicationService prnService;
    private final ObjectProvider<CurrentUser> currentUserProvider;

    public PrnMedicationController(PrnMedicationService prnService, ObjectProvider<CurrentUser> currentUserProvider) {
        this.prnService = prnService;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping({"/api/v1/don-thuoc/{id}/lan-dung-prn", "/api/v1/prescriptions/{id}/prn-administrations"})
    public PrnHistoryResponse getPrnHistory(@PathVariable("id") Long prescriptionId) {
        return prnService.getPrnHistory(prescriptionId);
    }

    @PostMapping({"/api/v1/don-thuoc/{id}/lan-dung-prn", "/api/v1/prescriptions/{id}/prn-administrations"})
    public ResponseEntity<PrnAdministrationResponse> recordPrn(
            @PathVariable("id") Long prescriptionId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody RecordPrnBody body) {

        CurrentUser currentUser = currentUserProvider.getIfAvailable();
        Long currentUserId = currentUser != null ? currentUser.accountId() : 1L;

        PrnAdministrationResponse res = prnService.recordPrn(prescriptionId, body, currentUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(res);
    }
}
