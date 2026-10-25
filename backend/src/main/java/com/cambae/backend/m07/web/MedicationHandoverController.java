package com.cambae.backend.m07.web;

import com.cambae.backend.common.error.web.ProblemBody;
import com.cambae.backend.m07.internal.service.DoseViewService;
import com.cambae.backend.m07.internal.service.MedicationHandoverService;
import com.cambae.backend.m07.internal.service.request.CreateHandoverBody;
import com.cambae.backend.m07.internal.service.request.HandoverItemBody;
import com.cambae.backend.m07.internal.service.request.NoMedicationOrderBody;
import com.cambae.backend.m07.internal.service.request.ReturnHandoverBody;
import com.cambae.backend.m07.internal.service.request.ReturnItemBody;
import com.cambae.backend.m07.web.request.DoseRequests.PersonType;
import com.cambae.backend.m07.web.request.HandoverRequests.NewMedicationHandoverRequest;
import com.cambae.backend.m07.web.request.HandoverRequests.NoMedicationOrderRequest;
import com.cambae.backend.m07.web.request.HandoverRequests.ReturnHandoverRequest;
import com.cambae.backend.m07.web.response.HandoverBodies.MedicationHandoverBody;
import com.cambae.backend.m07.web.response.HandoverBodies.MedicationToBringBody;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Thuốc mang theo khi vắng, đi chuyến theo api-006 (thẻ {@code leave-medication}; FR-047, FR-048a, FR-048b). */
@RestController
@Tag(name = "leave-medication")
public class MedicationHandoverController {

    private static final String PROBLEM = "application/problem+json";

    private final MedicationHandoverService handoverService;
    private final DoseViewService doseViews;
    private final CurrentUser currentUser;

    public MedicationHandoverController(
            MedicationHandoverService handoverService, DoseViewService doseViews, CurrentUser currentUser) {
        this.handoverService = handoverService;
        this.doseViews = doseViews;
        this.currentUser = currentUser;
    }

    @GetMapping("/api/v1/residents/{residentId}/medications-to-bring")
    @ApiResponse(responseCode = "200", description = "OK")
    public List<MedicationToBringBody> getMedicationsToBring(
            @PathVariable("residentId") Long residentId, @RequestParam(name = "tripId", required = false) Long tripId) {
        return handoverService.getMedicationsToBring(residentId, tripId).stream()
                .map(m -> MedicationToBringBody.of(
                        m, doseViews.usableBatches(residentId, m.getPrescriptionId(), m.getMedicationSource())))
                .toList();
    }

    @GetMapping("/api/v1/residents/{residentId}/medication-handovers")
    @ApiResponse(responseCode = "200", description = "OK")
    public List<MedicationHandoverBody> listHandovers(@PathVariable("residentId") Long residentId) {
        return handoverService.listHandovers(residentId).stream()
                .map(MedicationHandoverBody::of)
                .toList();
    }

    @PostMapping("/api/v1/residents/{residentId}/medication-handovers")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Đang mang theo", content = @Content),
        @ApiResponse(
                responseCode = "409",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class)))
    })
    public ResponseEntity<Void> createHandover(
            @PathVariable("residentId") Long residentId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody NewMedicationHandoverRequest body) {
        CreateHandoverBody command = new CreateHandoverBody();
        command.setTripId(body.tripId());
        // Người nhận nhân viên trên chuyến đi là trưởng đoàn (FR-048a)
        command.setRecipientType(
                body.recipient().type() == PersonType.STAFF
                        ? (body.tripId() != null ? "TRIP_LEADER" : "STAFF")
                        : "RELATIVE");
        command.setRecipientId(body.recipient().id());
        command.setRecipientName(body.recipient().fullName());
        command.setRelationship(body.recipient().relationship());
        command.setItems(body.items().stream()
                .map(i -> {
                    HandoverItemBody item = new HandoverItemBody();
                    item.setPrescriptionId(i.prescriptionId());
                    item.setQuantity(i.quantity());
                    item.setFamilyMedicationBatchId(i.familyMedicationBatchId());
                    return item;
                })
                .toList());
        handoverService.createHandover(residentId, command, currentUser.accountId());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/api/v1/medication-handovers/{id}/return")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Đã nhận lại", content = @Content),
        @ApiResponse(
                responseCode = "409",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class)))
    })
    public ResponseEntity<Void> returnHandover(
            @PathVariable("id") Long id,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody ReturnHandoverRequest body) {
        ReturnHandoverBody command = new ReturnHandoverBody();
        command.setItems(body.items().stream()
                .map(i -> {
                    ReturnItemBody item = new ReturnItemBody();
                    item.setPrescriptionId(i.prescriptionId());
                    item.setQuantity(i.quantity());
                    return item;
                })
                .toList());
        command.setReason(body.reason());
        handoverService.returnHandover(id, command, currentUser.accountId());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/api/v1/residents/{residentId}/no-medication-orders")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Đã ghi", content = @Content),
        @ApiResponse(
                responseCode = "409",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class)))
    })
    public ResponseEntity<Void> orderNoMedication(
            @PathVariable("residentId") Long residentId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody NoMedicationOrderRequest body) {
        NoMedicationOrderBody command = new NoMedicationOrderBody();
        command.setTripId(body.tripId());
        command.setReason(body.reason());
        handoverService.orderNoMedication(residentId, command, currentUser.accountId());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
