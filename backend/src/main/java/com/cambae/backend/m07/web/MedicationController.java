package com.cambae.backend.m07.web;

import com.cambae.backend.common.Reason;
import com.cambae.backend.common.web.VersionEtag;
import com.cambae.backend.m07.internal.service.MedicationService;
import com.cambae.backend.m07.internal.service.MedicationService.CreateMedicationCommand;
import com.cambae.backend.m07.internal.service.MedicationService.MedicationDetailView;
import com.cambae.backend.m07.internal.service.MedicationService.UpdateMedicationCommand;
import com.cambae.backend.m07.web.request.MedicationBody;
import com.cambae.backend.m07.web.request.ReasonBody;
import com.cambae.backend.m07.web.response.MedicationResponse;
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

/**
 * Medication Catalog Controller (FR-001, FR-002, FR-003, AC-01; contracts/api-006.yaml section danh-muc-thuoc).
 * Supports legacy endpoint {@code /api/v1/thuoc} and English endpoint {@code /api/v1/medications}.
 */
@RestController
@RequestMapping({"/api/v1/thuoc", "/api/v1/medications"})
public class MedicationController {

    private final MedicationService medicationService;

    public MedicationController(MedicationService medicationService) {
        this.medicationService = medicationService;
    }

    @GetMapping
    public List<MedicationResponse> list(
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "hieuLuc", required = false) Boolean activeOnly) {
        return medicationService.list(q, activeOnly).stream()
                .map(MedicationResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<MedicationResponse> getById(@PathVariable Long id) {
        MedicationDetailView view = medicationService.getById(id);
        return ResponseEntity.ok().eTag(VersionEtag.of(view.version())).body(MedicationResponse.from(view));
    }

    @PostMapping
    public ResponseEntity<MedicationResponse> create(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody MedicationBody body) {
        CreateMedicationCommand cmd = new CreateMedicationCommand(
                body.getName(),
                body.getConcentration(),
                body.getDosageForm(),
                body.getDefaultRoute(),
                body.getUsageUnit(),
                body.getAllergenIds() == null ? Set.of() : Set.copyOf(body.getAllergenIds()),
                body.getBeforeWindowMinutes(),
                body.getAfterWindowMinutes(),
                body.isSpecialControl());

        MedicationDetailView created = medicationService.create(cmd);
        return ResponseEntity.status(HttpStatus.CREATED)
                .eTag(VersionEtag.of(created.version()))
                .body(MedicationResponse.from(created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MedicationResponse> update(
            @PathVariable Long id,
            @RequestHeader(value = "If-Match", required = true) String ifMatch,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody MedicationBody body) {
        UpdateMedicationCommand cmd = new UpdateMedicationCommand(
                body.getName(),
                body.getConcentration(),
                body.getDosageForm(),
                body.getDefaultRoute(),
                body.getUsageUnit(),
                body.getAllergenIds() == null ? Set.of() : Set.copyOf(body.getAllergenIds()),
                body.getBeforeWindowMinutes(),
                body.getAfterWindowMinutes(),
                body.isSpecialControl());

        MedicationDetailView updated = medicationService.update(id, ifMatch, cmd);
        return ResponseEntity.ok().eTag(VersionEtag.of(updated.version())).body(MedicationResponse.from(updated));
    }

    @PostMapping({"/{id}/ngung-hieu-luc", "/{id}/deactivate"})
    public ResponseEntity<MedicationResponse> deactivate(
            @PathVariable Long id,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody(required = false) ReasonBody body) {
        Reason reason = body != null && body.reason() != null && !body.reason().isBlank()
                ? Reason.of(body.reason())
                : Reason.of("Deactivated catalog item");
        MedicationDetailView deactivated = medicationService.deactivate(id, reason);
        return ResponseEntity.ok()
                .eTag(VersionEtag.of(deactivated.version()))
                .body(MedicationResponse.from(deactivated));
    }
}
