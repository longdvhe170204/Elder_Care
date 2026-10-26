package com.cambae.backend.m07.web;

import com.cambae.backend.m07.internal.domain.NoMedicationOrder;
import com.cambae.backend.m07.internal.service.MedicationHandoverService;
import com.cambae.backend.m07.web.request.CreateHandoverBody;
import com.cambae.backend.m07.web.request.NoMedicationOrderBody;
import com.cambae.backend.m07.web.request.ReportedDoseBody;
import com.cambae.backend.m07.web.request.ReturnHandoverBody;
import com.cambae.backend.m07.web.response.HandoverResponse;
import com.cambae.backend.m07.web.response.MedicationToBringResponse;
import com.cambae.backend.m15.account.api.CurrentUser;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for carried medication and absence intervals (FR-021, FR-027, FR-048a, FR-048b; contracts/api-006.yaml section mang-theo).
 */
@RestController
public class MedicationHandoverController {

    private final MedicationHandoverService handoverService;
    private final ObjectProvider<CurrentUser> currentUserProvider;

    public MedicationHandoverController(
            MedicationHandoverService handoverService, ObjectProvider<CurrentUser> currentUserProvider) {
        this.handoverService = handoverService;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping({"/api/v1/nguoi-cao-tuoi/{nctId}/thuoc-can-mang", "/api/v1/residents/{nctId}/medications-to-bring"})
    public List<MedicationToBringResponse> getMedicationsToBring(
            @PathVariable("nctId") Long residentId, @RequestParam(name = "chuyenId", required = false) Long tripId) {
        return handoverService.getMedicationsToBring(residentId, tripId);
    }

    @GetMapping({"/api/v1/nguoi-cao-tuoi/{nctId}/lan-giao-thuoc", "/api/v1/residents/{nctId}/medication-handovers"})
    public List<HandoverResponse> listHandovers(@PathVariable("nctId") Long residentId) {
        return handoverService.listHandovers(residentId);
    }

    @PostMapping({"/api/v1/nguoi-cao-tuoi/{nctId}/lan-giao-thuoc", "/api/v1/residents/{nctId}/medication-handovers"})
    public ResponseEntity<HandoverResponse> createHandover(
            @PathVariable("nctId") Long residentId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody CreateHandoverBody body) {

        CurrentUser currentUser = currentUserProvider.getIfAvailable();
        Long currentUserId = currentUser != null ? currentUser.accountId() : 1L;

        HandoverResponse res = handoverService.createHandover(residentId, body, currentUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(res);
    }

    @PostMapping({"/api/v1/lan-giao-thuoc/{id}/nhan-lai", "/api/v1/medication-handovers/{id}/return"})
    public ResponseEntity<HandoverResponse> returnHandover(
            @PathVariable("id") Long id,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody ReturnHandoverBody body) {

        CurrentUser currentUser = currentUserProvider.getIfAvailable();
        Long currentUserId = currentUser != null ? currentUser.accountId() : 1L;

        HandoverResponse res = handoverService.returnHandover(id, body, currentUserId);
        return ResponseEntity.ok(res);
    }

    @PostMapping({"/api/v1/nguoi-cao-tuoi/{nctId}/khong-mang-thuoc", "/api/v1/residents/{nctId}/no-medication-orders"})
    public ResponseEntity<NoMedicationOrder> orderNoMedication(
            @PathVariable("nctId") Long residentId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody NoMedicationOrderBody body) {

        CurrentUser currentUser = currentUserProvider.getIfAvailable();
        Long currentUserId = currentUser != null ? currentUser.accountId() : 1L;

        NoMedicationOrder order = handoverService.orderNoMedication(residentId, body, currentUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }

    @PostMapping({"/api/v1/lieu-thuoc/{id}/bao-lai", "/api/v1/doses/{id}/reported"})
    public ResponseEntity<?> recordReportedDose(
            @PathVariable("id") Long id, @Valid @RequestBody ReportedDoseBody body) {

        CurrentUser currentUser = currentUserProvider.getIfAvailable();
        Long currentUserId = currentUser != null ? currentUser.accountId() : 1L;
        boolean isNurse =
                currentUser == null || currentUser.roles().contains(com.cambae.backend.common.Role.NURSING_STAFF);

        var dose = handoverService.recordReportedDose(id, body, currentUserId, isNurse);
        return ResponseEntity.ok(dose);
    }
}
