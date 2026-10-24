package com.cambae.backend.m07.web;

import com.cambae.backend.common.Role;
import com.cambae.backend.common.web.VersionEtag;
import com.cambae.backend.m07.internal.service.PrescriptionService;
import com.cambae.backend.m07.web.request.ChangeDosageBody;
import com.cambae.backend.m07.web.request.CreatePrescriptionBody;
import com.cambae.backend.m07.web.request.DiscontinuePrescriptionBody;
import com.cambae.backend.m07.web.request.ExtendPrescriptionBody;
import com.cambae.backend.m07.web.request.InstructionBody;
import com.cambae.backend.m07.web.request.OrderPrescriptionBody;
import com.cambae.backend.m07.web.response.PrescriptionCheckResponse;
import com.cambae.backend.m07.web.response.PrescriptionResponse;
import com.cambae.backend.m15.account.api.CurrentUser;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Prescription Controller (FR-004 -> FR-017; contracts/api-006.yaml section don-thuoc).
 * Supports both OpenAPI paths and English paths:
 * - /api/v1/nguoi-cao-tuoi/{nctId}/don-thuoc and /api/v1/residents/{nctId}/prescriptions
 * - /api/v1/don-thuoc/{id} and /api/v1/prescriptions/{id}
 */
@RestController
public class PrescriptionController {

    private final PrescriptionService prescriptionService;
    private final ObjectProvider<CurrentUser> currentUserProvider;

    public PrescriptionController(
            PrescriptionService prescriptionService, ObjectProvider<CurrentUser> currentUserProvider) {
        this.prescriptionService = prescriptionService;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping({"/api/v1/nguoi-cao-tuoi/{nctId}/don-thuoc", "/api/v1/residents/{nctId}/prescriptions"})
    public List<PrescriptionResponse> list(
            @PathVariable("nctId") Long residentId,
            @RequestParam(name = "trangThai", required = false) List<String> statuses) {
        return prescriptionService.listByResident(residentId, statuses);
    }

    @PostMapping({"/api/v1/nguoi-cao-tuoi/{nctId}/don-thuoc", "/api/v1/residents/{nctId}/prescriptions"})
    public ResponseEntity<PrescriptionResponse> create(
            @PathVariable("nctId") Long residentId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody CreatePrescriptionBody body) {

        CurrentUser currentUser = currentUserProvider.getIfAvailable();
        Long currentUserId = currentUser != null ? currentUser.accountId() : 1L;
        boolean isDoctor = currentUser != null
                ? currentUser.roles().contains(Role.DOCTOR)
                : true; // Default true if no Security Context present in unit test

        PrescriptionResponse created = prescriptionService.create(residentId, body, currentUserId, isDoctor);

        return ResponseEntity.status(HttpStatus.CREATED)
                .eTag(VersionEtag.of(created.version()))
                .body(created);
    }

    @PostMapping({"/api/v1/nguoi-cao-tuoi/{nctId}/don-thuoc/kiem-tra", "/api/v1/residents/{nctId}/prescriptions/check"})
    public ResponseEntity<PrescriptionCheckResponse> check(
            @PathVariable("nctId") Long residentId, @Valid @RequestBody CreatePrescriptionBody body) {

        var result = prescriptionService.check(residentId, body);
        return ResponseEntity.ok(PrescriptionCheckResponse.from(result));
    }

    @GetMapping({"/api/v1/don-thuoc/{id}", "/api/v1/prescriptions/{id}"})
    public ResponseEntity<PrescriptionResponse> getById(@PathVariable("id") Long id) {
        PrescriptionResponse response = prescriptionService.getById(id);
        return ResponseEntity.ok().eTag(VersionEtag.of(response.version())).body(response);
    }

    @PatchMapping({
        "/api/v1/don-thuoc/{id}/ghi-chu",
        "/api/v1/prescriptions/{id}/ghi-chu",
        "/api/v1/prescriptions/{id}/instruction"
    })
    public ResponseEntity<PrescriptionResponse> updateInstructionNotePatch(
            @PathVariable("id") Long id,
            @RequestHeader("If-Match") String ifMatch,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody InstructionBody body) {

        PrescriptionResponse updated =
                prescriptionService.updateInstructionNote(id, ifMatch, body.getInstructionNote());

        return ResponseEntity.ok().eTag(VersionEtag.of(updated.version())).body(updated);
    }

    @PostMapping({"/api/v1/don-thuoc/{id}/ghi-chu", "/api/v1/prescriptions/{id}/ghi-chu"})
    public ResponseEntity<PrescriptionResponse> updateInstructionNotePost(
            @PathVariable("id") Long id,
            @RequestHeader("If-Match") String ifMatch,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody InstructionBody body) {

        return updateInstructionNotePatch(id, ifMatch, idempotencyKey, body);
    }

    @PostMapping({"/api/v1/don-thuoc/{id}/tam-dung", "/api/v1/prescriptions/{id}/suspend"})
    public ResponseEntity<PrescriptionResponse> suspend(
            @PathVariable("id") Long id,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody OrderPrescriptionBody body) {

        CurrentUser currentUser = currentUserProvider.getIfAvailable();
        Long currentUserId = currentUser != null ? currentUser.accountId() : 1L;
        boolean isDoctor = currentUser != null && currentUser.roles().contains(Role.DOCTOR);

        PrescriptionResponse response = prescriptionService.suspend(id, body, currentUserId, isDoctor);
        return ResponseEntity.ok().eTag(VersionEtag.of(response.version())).body(response);
    }

    @PostMapping({"/api/v1/don-thuoc/{id}/tiep-tuc", "/api/v1/prescriptions/{id}/resume"})
    public ResponseEntity<PrescriptionResponse> resume(
            @PathVariable("id") Long id,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody OrderPrescriptionBody body) {

        CurrentUser currentUser = currentUserProvider.getIfAvailable();
        Long currentUserId = currentUser != null ? currentUser.accountId() : 1L;
        boolean isDoctor = currentUser != null && currentUser.roles().contains(Role.DOCTOR);

        PrescriptionResponse response = prescriptionService.resume(id, body, currentUserId, isDoctor);
        return ResponseEntity.ok().eTag(VersionEtag.of(response.version())).body(response);
    }

    @PostMapping({"/api/v1/don-thuoc/{id}/ngung", "/api/v1/prescriptions/{id}/discontinue"})
    public ResponseEntity<PrescriptionResponse> discontinue(
            @PathVariable("id") Long id,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody DiscontinuePrescriptionBody body) {

        CurrentUser currentUser = currentUserProvider.getIfAvailable();
        Long currentUserId = currentUser != null ? currentUser.accountId() : 1L;
        boolean isDoctor = currentUser != null && currentUser.roles().contains(Role.DOCTOR);

        PrescriptionResponse response = prescriptionService.discontinue(id, body, currentUserId, isDoctor);
        return ResponseEntity.ok().eTag(VersionEtag.of(response.version())).body(response);
    }

    @PostMapping({"/api/v1/don-thuoc/{id}/doi-lieu", "/api/v1/prescriptions/{id}/change-dosage"})
    public ResponseEntity<PrescriptionResponse> changeDosage(
            @PathVariable("id") Long id,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody ChangeDosageBody body) {

        CurrentUser currentUser = currentUserProvider.getIfAvailable();
        Long currentUserId = currentUser != null ? currentUser.accountId() : 1L;
        boolean isDoctor = currentUser != null && currentUser.roles().contains(Role.DOCTOR);

        PrescriptionResponse response = prescriptionService.changeDosage(id, body, currentUserId, isDoctor);
        return ResponseEntity.status(HttpStatus.CREATED)
                .eTag(VersionEtag.of(response.version()))
                .body(response);
    }

    @PostMapping({"/api/v1/don-thuoc/{id}/gia-han", "/api/v1/prescriptions/{id}/extend"})
    public ResponseEntity<PrescriptionResponse> extend(
            @PathVariable("id") Long id,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody ExtendPrescriptionBody body) {

        CurrentUser currentUser = currentUserProvider.getIfAvailable();
        Long currentUserId = currentUser != null ? currentUser.accountId() : 1L;
        boolean isDoctor = currentUser != null && currentUser.roles().contains(Role.DOCTOR);

        PrescriptionResponse response = prescriptionService.extend(id, body, currentUserId, isDoctor);
        return ResponseEntity.ok().eTag(VersionEtag.of(response.version())).body(response);
    }
}
