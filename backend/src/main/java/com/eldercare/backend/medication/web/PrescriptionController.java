package com.eldercare.backend.medication.web;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.error.web.ProblemBody;
import com.eldercare.backend.common.web.VersionEtag;
import com.eldercare.backend.medication.internal.service.PrescriptionService;
import com.eldercare.backend.medication.internal.service.response.PrescriptionCheckResponse;
import com.eldercare.backend.medication.internal.service.response.PrescriptionResponse;
import com.eldercare.backend.medication.web.request.NewPrescriptionRequest;
import com.eldercare.backend.medication.web.request.PrescriptionChangeRequests.ChangeDosageRequest;
import com.eldercare.backend.medication.web.request.PrescriptionChangeRequests.DiscontinueRequest;
import com.eldercare.backend.medication.web.request.PrescriptionChangeRequests.ExtendRequest;
import com.eldercare.backend.medication.web.request.PrescriptionChangeRequests.InstructionNoteRequest;
import com.eldercare.backend.medication.web.request.PrescriptionCommandRequest;
import com.eldercare.backend.medication.web.response.PrescriptionBodies.CheckBody;
import com.eldercare.backend.medication.web.response.PrescriptionBodies.DiscontinueBody;
import com.eldercare.backend.medication.web.response.PrescriptionBodies.PrescriptionBody;
import com.eldercare.backend.medication.web.response.PrescriptionBodies.Warning;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Đơn thuốc theo api-006 (thẻ {@code prescription}; FR-004 → FR-017). */
@RestController
@Tag(name = "prescription")
public class PrescriptionController {

    private static final String PROBLEM = "application/problem+json";

    private final PrescriptionService prescriptionService;
    private final CurrentUser currentUser;

    public PrescriptionController(PrescriptionService prescriptionService, CurrentUser currentUser) {
        this.prescriptionService = prescriptionService;
        this.currentUser = currentUser;
    }

    private boolean isDoctor() {
        return currentUser.roles().contains(Role.DOCTOR);
    }

    @GetMapping("/api/v1/residents/{residentId}/prescriptions")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(
                responseCode = "403",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class)))
    })
    public List<PrescriptionBody> list(
            @PathVariable("residentId") Long residentId,
            @RequestParam(name = "status", required = false) List<String> status) {
        return prescriptionService.listByResident(PrescriptionService.target(residentId), residentId, status).stream()
                .map(PrescriptionBody::of)
                .toList();
    }

    @PostMapping("/api/v1/residents/{residentId}/prescriptions")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Hiệu lực hoặc Chờ hiệu lực"),
        @ApiResponse(
                responseCode = "403",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class))),
        @ApiResponse(
                responseCode = "409",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class))),
        @ApiResponse(
                responseCode = "422",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class)))
    })
    public ResponseEntity<PrescriptionBody> create(
            @PathVariable("residentId") Long residentId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody NewPrescriptionRequest body) {
        PrescriptionResponse created = prescriptionService.create(
                PrescriptionService.target(residentId),
                residentId,
                M07RequestMapper.prescription(body),
                currentUser.accountId(),
                isDoctor());
        return ResponseEntity.status(HttpStatus.CREATED)
                .eTag(VersionEtag.of(created.version()))
                .body(PrescriptionBody.of(created));
    }

    @PostMapping("/api/v1/residents/{residentId}/prescriptions/check")
    @ApiResponse(responseCode = "200", description = "OK")
    public CheckBody check(
            @PathVariable("residentId") Long residentId, @Valid @RequestBody NewPrescriptionRequest body) {
        return CheckBody.of(PrescriptionCheckResponse.from(prescriptionService.check(
                PrescriptionService.target(residentId), residentId, M07RequestMapper.prescription(body))));
    }

    @GetMapping("/api/v1/prescriptions/{id}")
    @ApiResponse(responseCode = "200", description = "OK")
    public ResponseEntity<PrescriptionBody> getById(@PathVariable("id") Long id) {
        PrescriptionResponse response = prescriptionService.getById(prescriptionService.targetOf(id), id);
        return ResponseEntity.ok().eTag(VersionEtag.of(response.version())).body(PrescriptionBody.of(response));
    }

    @PatchMapping("/api/v1/prescriptions/{id}/instruction-note")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK", content = @Content),
        @ApiResponse(
                responseCode = "412",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class)))
    })
    public ResponseEntity<Void> updateInstructionNote(
            @PathVariable("id") Long id,
            @RequestHeader("If-Match") String ifMatch,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody InstructionNoteRequest body) {
        PrescriptionResponse updated = prescriptionService.updateInstructionNote(
                prescriptionService.targetOf(id), id, ifMatch, body.instructionNote());
        return ResponseEntity.ok().eTag(VersionEtag.of(updated.version())).build();
    }

    @PostMapping("/api/v1/prescriptions/{id}/suspend")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK", content = @Content),
        @ApiResponse(
                responseCode = "409",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class))),
        @ApiResponse(
                responseCode = "422",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class)))
    })
    public ResponseEntity<Void> suspend(
            @PathVariable("id") Long id,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody PrescriptionCommandRequest body) {
        PrescriptionResponse response = prescriptionService.suspend(
                prescriptionService.targetOf(id),
                id,
                M07RequestMapper.order(body),
                currentUser.accountId(),
                isDoctor());
        return ResponseEntity.ok().eTag(VersionEtag.of(response.version())).build();
    }

    @PostMapping("/api/v1/prescriptions/{id}/resume")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK", content = @Content),
        @ApiResponse(
                responseCode = "409",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class)))
    })
    public ResponseEntity<Void> resume(
            @PathVariable("id") Long id,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody PrescriptionCommandRequest body) {
        PrescriptionResponse response = prescriptionService.resume(
                prescriptionService.targetOf(id),
                id,
                M07RequestMapper.order(body),
                currentUser.accountId(),
                isDoctor());
        return ResponseEntity.ok().eTag(VersionEtag.of(response.version())).build();
    }

    @PostMapping("/api/v1/prescriptions/{id}/discontinue")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(
                responseCode = "409",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class)))
    })
    public ResponseEntity<DiscontinueBody> discontinue(
            @PathVariable("id") Long id,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody DiscontinueRequest body) {
        List<Warning> warnings = prescriptionService.discontinueWarnings(id, body.discontinueAt()).stream()
                .map(Warning::new)
                .toList();
        PrescriptionResponse response = prescriptionService.discontinue(
                prescriptionService.targetOf(id),
                id,
                M07RequestMapper.discontinue(body),
                currentUser.accountId(),
                isDoctor());
        return ResponseEntity.ok().eTag(VersionEtag.of(response.version())).body(new DiscontinueBody(warnings));
    }

    @PostMapping("/api/v1/prescriptions/{id}/change-dosage")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Đơn thay thế"),
        @ApiResponse(
                responseCode = "409",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class)))
    })
    public ResponseEntity<PrescriptionBody> changeDosage(
            @PathVariable("id") Long id,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody ChangeDosageRequest body) {
        PrescriptionResponse replacement = prescriptionService.changeDosage(
                prescriptionService.targetOf(id),
                id,
                M07RequestMapper.changeDosage(body),
                currentUser.accountId(),
                isDoctor());
        return ResponseEntity.status(HttpStatus.CREATED)
                .eTag(VersionEtag.of(replacement.version()))
                .body(PrescriptionBody.of(replacement));
    }

    @PostMapping("/api/v1/prescriptions/{id}/extend")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK", content = @Content),
        @ApiResponse(
                responseCode = "409",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class)))
    })
    public ResponseEntity<Void> extend(
            @PathVariable("id") Long id,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody ExtendRequest body) {
        PrescriptionResponse response = prescriptionService.extend(
                prescriptionService.targetOf(id),
                id,
                M07RequestMapper.extend(body),
                currentUser.accountId(),
                isDoctor());
        return ResponseEntity.ok().eTag(VersionEtag.of(response.version())).build();
    }
}
