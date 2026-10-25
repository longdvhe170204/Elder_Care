package com.cambae.backend.m07.web;

import com.cambae.backend.m07.internal.service.FamilyMedicationService;
import com.cambae.backend.m07.web.request.*;
import com.cambae.backend.m07.web.response.FamilyMedicationBatchResponse;
import com.cambae.backend.m15.account.api.CurrentUser;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class FamilyMedicationController {

    private final FamilyMedicationService familyMedicationService;
    private final ObjectProvider<CurrentUser> currentUserProvider;

    public FamilyMedicationController(
            FamilyMedicationService familyMedicationService, ObjectProvider<CurrentUser> currentUserProvider) {
        this.familyMedicationService = familyMedicationService;
        this.currentUserProvider = currentUserProvider;
    }

    private Long getCurrentUserId() {
        CurrentUser currentUser = currentUserProvider.getIfAvailable();
        return currentUser != null ? currentUser.accountId() : 1L;
    }

    @GetMapping({"/api/v1/nguoi-cao-tuoi/{nctId}/thuoc-gia-dinh-gui", "/api/v1/residents/{nctId}/family-medications"})
    public ResponseEntity<List<FamilyMedicationBatchResponse>> listBatches(@PathVariable("nctId") Long residentId) {
        return ResponseEntity.ok(familyMedicationService.listBatches(residentId));
    }

    @PostMapping({"/api/v1/nguoi-cao-tuoi/{nctId}/thuoc-gia-dinh-gui", "/api/v1/residents/{nctId}/family-medications"})
    public ResponseEntity<FamilyMedicationBatchResponse> receiveBatch(
            @PathVariable("nctId") Long residentId, @Valid @RequestBody ReceiveBatchBody body) {
        FamilyMedicationBatchResponse response =
                familyMedicationService.receiveBatch(residentId, body, getCurrentUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping({"/api/v1/thuoc-gia-dinh-gui/{id}", "/api/v1/family-medications/{id}"})
    public ResponseEntity<FamilyMedicationBatchResponse> getBatch(@PathVariable("id") Long id) {
        return ResponseEntity.ok(familyMedicationService.getBatch(id));
    }

    @PostMapping({"/api/v1/thuoc-gia-dinh-gui/{id}/xac-nhan-su-dung", "/api/v1/family-medications/{id}/confirm-use"})
    public ResponseEntity<FamilyMedicationBatchResponse> confirmUse(
            @PathVariable("id") Long id, @Valid @RequestBody ConfirmBatchUseBody body) {
        return ResponseEntity.ok(familyMedicationService.confirmUse(id, body, getCurrentUserId()));
    }

    @PostMapping({"/api/v1/thuoc-gia-dinh-gui/{id}/chi-giu-ho", "/api/v1/family-medications/{id}/hold-only"})
    public ResponseEntity<FamilyMedicationBatchResponse> holdOnly(
            @PathVariable("id") Long id, @RequestBody(required = false) Map<String, String> body) {
        String reason = body != null ? body.getOrDefault("lyDo", body.get("reason")) : null;
        return ResponseEntity.ok(familyMedicationService.holdOnly(id, reason, getCurrentUserId()));
    }

    @PostMapping({"/api/v1/thuoc-gia-dinh-gui/{id}/ngung-su-dung", "/api/v1/family-medications/{id}/stop-use"})
    public ResponseEntity<FamilyMedicationBatchResponse> stopUse(
            @PathVariable("id") Long id, @RequestBody(required = false) Map<String, String> body) {
        String reason = body != null ? body.getOrDefault("lyDo", body.get("reason")) : null;
        return ResponseEntity.ok(familyMedicationService.stopUse(id, reason, getCurrentUserId()));
    }

    @PostMapping({
        "/api/v1/thuoc-gia-dinh-gui/{id}/kiem-ke-dieu-chinh",
        "/api/v1/family-medications/{id}/adjust-inventory"
    })
    public ResponseEntity<FamilyMedicationBatchResponse> adjustInventory(
            @PathVariable("id") Long id, @Valid @RequestBody AdjustInventoryBody body) {
        return ResponseEntity.ok(familyMedicationService.adjustInventory(id, body, getCurrentUserId()));
    }

    @PostMapping({"/api/v1/thuoc-gia-dinh-gui/{id}/hoan-tra", "/api/v1/family-medications/{id}/return"})
    public ResponseEntity<FamilyMedicationBatchResponse> returnBatch(
            @PathVariable("id") Long id, @Valid @RequestBody ReturnBatchBody body) {
        return ResponseEntity.ok(familyMedicationService.returnBatch(id, body, getCurrentUserId()));
    }

    @PostMapping({"/api/v1/thuoc-gia-dinh-gui/{id}/huy", "/api/v1/family-medications/{id}/dispose"})
    public ResponseEntity<FamilyMedicationBatchResponse> disposeBatch(
            @PathVariable("id") Long id, @Valid @RequestBody DisposeBatchBody body) {
        return ResponseEntity.ok(familyMedicationService.disposeBatch(id, body, getCurrentUserId()));
    }
}
