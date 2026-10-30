package com.eldercare.backend.medication.web;

import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.error.web.ProblemBody;
import com.eldercare.backend.common.web.VersionEtag;
import com.eldercare.backend.medication.internal.service.MedicationService;
import com.eldercare.backend.medication.internal.service.MedicationService.CreateMedicationCommand;
import com.eldercare.backend.medication.internal.service.MedicationService.MedicationDetailView;
import com.eldercare.backend.medication.internal.service.MedicationService.UpdateMedicationCommand;
import com.eldercare.backend.medication.web.request.MedicationRequest;
import com.eldercare.backend.medication.web.request.ReasonRequest;
import com.eldercare.backend.medication.web.response.MedicationBody;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Danh mục thuốc theo api-006 (thẻ {@code medication-catalog}; FR-001 → FR-003). */
@RestController
@RequestMapping("/api/v1/medications")
@Tag(name = "medication-catalog")
public class MedicationController {

    private final MedicationService medicationService;

    public MedicationController(MedicationService medicationService) {
        this.medicationService = medicationService;
    }

    @GetMapping
    @ApiResponse(responseCode = "200", description = "OK")
    public List<MedicationBody> list(
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "active", required = false) Boolean active) {
        return medicationService.list(q, active).stream()
                .map(MedicationBody::of)
                .toList();
    }

    @GetMapping("/{id}")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(
                responseCode = "404",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = ProblemBody.class)))
    })
    public ResponseEntity<MedicationBody> getById(@PathVariable("id") Long id) {
        MedicationDetailView view = medicationService.getById(id);
        return ResponseEntity.ok().eTag(VersionEtag.of(view.version())).body(MedicationBody.of(view));
    }

    @PostMapping
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Đã thêm", content = @Content),
        @ApiResponse(
                responseCode = "403",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = ProblemBody.class))),
        @ApiResponse(
                responseCode = "409",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = ProblemBody.class)))
    })
    public ResponseEntity<Void> create(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody MedicationRequest body) {
        MedicationDetailView created = medicationService.create(new CreateMedicationCommand(
                body.name(),
                body.concentration(),
                body.dosageForm(),
                body.defaultRoute(),
                body.usageUnit(),
                Set.copyOf(body.allergenIds()),
                body.beforeWindowMinutes(),
                body.afterWindowMinutes(),
                Boolean.TRUE.equals(body.specialControl())));
        return ResponseEntity.status(HttpStatus.CREATED)
                .eTag(VersionEtag.of(created.version()))
                .build();
    }

    @PutMapping("/{id}")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK", content = @Content),
        @ApiResponse(
                responseCode = "412",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = ProblemBody.class)))
    })
    public ResponseEntity<Void> update(
            @PathVariable("id") Long id,
            @RequestHeader("If-Match") String ifMatch,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody MedicationRequest body) {
        MedicationDetailView updated = medicationService.update(
                id,
                ifMatch,
                new UpdateMedicationCommand(
                        body.name(),
                        body.concentration(),
                        body.dosageForm(),
                        body.defaultRoute(),
                        body.usageUnit(),
                        Set.copyOf(body.allergenIds()),
                        body.beforeWindowMinutes(),
                        body.afterWindowMinutes(),
                        Boolean.TRUE.equals(body.specialControl())));
        return ResponseEntity.ok().eTag(VersionEtag.of(updated.version())).build();
    }

    @PostMapping("/{id}/deactivate")
    @ApiResponse(responseCode = "200", description = "OK", content = @Content)
    public ResponseEntity<Void> deactivate(
            @PathVariable("id") Long id,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody ReasonRequest body) {
        MedicationDetailView deactivated = medicationService.deactivate(id, Reason.of(body.reason()));
        return ResponseEntity.ok().eTag(VersionEtag.of(deactivated.version())).build();
    }
}
