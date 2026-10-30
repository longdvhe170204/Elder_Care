package com.cambae.backend.m07.web;

import com.cambae.backend.common.error.web.ProblemBody;
import com.cambae.backend.m07.internal.service.FamilyMedicationService;
import com.cambae.backend.m07.internal.service.request.AdjustInventoryBody;
import com.cambae.backend.m07.internal.service.request.ConfirmBatchUseBody;
import com.cambae.backend.m07.internal.service.request.DisposeBatchBody;
import com.cambae.backend.m07.internal.service.request.ReceiveBatchBody;
import com.cambae.backend.m07.internal.service.request.ReturnBatchBody;
import com.cambae.backend.m07.web.request.DoseRequests.PersonType;
import com.cambae.backend.m07.web.request.FamilyMedicationRequests.AdjustInventoryRequest;
import com.cambae.backend.m07.web.request.FamilyMedicationRequests.ConfirmUseRequest;
import com.cambae.backend.m07.web.request.FamilyMedicationRequests.DisposeRequest;
import com.cambae.backend.m07.web.request.FamilyMedicationRequests.ReceiveFamilyMedicationRequest;
import com.cambae.backend.m07.web.request.FamilyMedicationRequests.ReturnFamilyMedicationRequest;
import com.cambae.backend.m07.web.request.ReasonRequest;
import com.cambae.backend.m07.web.response.FamilyMedicationBatchBody;
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
import org.springframework.web.bind.annotation.RestController;

/** Thuốc gia đình gửi theo api-006 (thẻ {@code family-medication}; FR-040 → FR-046). */
@RestController
@Tag(name = "family-medication")
public class FamilyMedicationController {

    private static final String PROBLEM = "application/problem+json";

    private final FamilyMedicationService familyMedicationService;
    private final CurrentUser currentUser;

    public FamilyMedicationController(FamilyMedicationService familyMedicationService, CurrentUser currentUser) {
        this.familyMedicationService = familyMedicationService;
        this.currentUser = currentUser;
    }

    @GetMapping("/api/v1/residents/{residentId}/family-medications")
    @ApiResponse(responseCode = "200", description = "OK")
    public List<FamilyMedicationBatchBody> listBatches(@PathVariable("residentId") Long residentId) {
        return familyMedicationService.listBatches(residentId).stream()
                .map(FamilyMedicationBatchBody::of)
                .toList();
    }

    @PostMapping("/api/v1/residents/{residentId}/family-medications")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Chờ đối chiếu", content = @Content),
        @ApiResponse(
                responseCode = "409",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class)))
    })
    public ResponseEntity<Void> receiveBatch(
            @PathVariable("residentId") Long residentId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody ReceiveFamilyMedicationRequest body) {
        ReceiveBatchBody command = new ReceiveBatchBody();
        command.setMedicationId(body.medicationId());
        command.setCustomName(body.customName());
        command.setCustomActiveIngredient(body.customActiveIngredient());
        command.setCustomConcentration(body.concentration());
        command.setInitialQuantity(body.quantity());
        command.setUnit(body.unit());
        command.setExpiryDate(body.expiryDate());
        command.setPackagingCondition(body.packagingCondition());
        command.setHasPrescription(body.hasPrescription());
        command.setFileIds(body.fileIds());
        // Người giao là nhân viên mua hộ khi có đề nghị mua hộ (Q-139)
        command.setDelivererType(
                body.deliverer().type() == PersonType.STAFF
                        ? "PURCHASING_STAFF"
                        : body.deliverer().type() == null
                                ? null
                                : body.deliverer().type().name());
        command.setDelivererId(body.deliverer().id());
        command.setDelivererName(body.deliverer().fullName());
        command.setRelationship(body.deliverer().relationship());
        command.setPurchaseRequestId(body.purchaseAssistanceId());
        command.setReceivedAt(body.receivedAt());
        familyMedicationService.receiveBatch(residentId, command, currentUser.accountId());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/api/v1/family-medications/{id}")
    @ApiResponse(responseCode = "200", description = "OK")
    public FamilyMedicationBatchBody getBatch(@PathVariable("id") Long id) {
        return FamilyMedicationBatchBody.of(familyMedicationService.getBatch(id));
    }

    @PostMapping("/api/v1/family-medications/{id}/confirm-use")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Được sử dụng", content = @Content),
        @ApiResponse(
                responseCode = "409",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class)))
    })
    public ResponseEntity<Void> confirmUse(
            @PathVariable("id") Long id,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody ConfirmUseRequest body) {
        ConfirmBatchUseBody command = new ConfirmBatchUseBody();
        command.setPrescriptionIds(body.prescriptionIds());
        command.setMedicationId(body.medicationId());
        command.setPackagingRecognizable(body.packagingRecognizable());
        command.setBasis(body.basis() == null ? null : body.basis().name());
        familyMedicationService.confirmUse(id, command, currentUser.accountId());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/api/v1/family-medications/{id}/hold-only")
    @ApiResponse(responseCode = "200", description = "Chỉ giữ hộ", content = @Content)
    public ResponseEntity<Void> holdOnly(
            @PathVariable("id") Long id,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody ReasonRequest body) {
        familyMedicationService.holdOnly(id, body.reason(), currentUser.accountId());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/api/v1/family-medications/{id}/stop-use")
    @ApiResponse(responseCode = "200", description = "Chỉ giữ hộ", content = @Content)
    public ResponseEntity<Void> stopUse(
            @PathVariable("id") Long id,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody ReasonRequest body) {
        familyMedicationService.stopUse(id, body.reason(), currentUser.accountId());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/api/v1/family-medications/{id}/adjust-inventory")
    @ApiResponse(responseCode = "200", description = "OK", content = @Content)
    public ResponseEntity<Void> adjustInventory(
            @PathVariable("id") Long id,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody AdjustInventoryRequest body) {
        AdjustInventoryBody command = new AdjustInventoryBody();
        command.setActualQuantity(body.actualQuantity());
        command.setReason(body.reason());
        familyMedicationService.adjustInventory(id, command, currentUser.accountId());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/api/v1/family-medications/{id}/return")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Đã hoàn trả", content = @Content),
        @ApiResponse(
                responseCode = "409",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class)))
    })
    public ResponseEntity<Void> returnBatch(
            @PathVariable("id") Long id,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody ReturnFamilyMedicationRequest body) {
        ReturnBatchBody command = new ReturnBatchBody();
        command.setQuantity(body.quantity());
        command.setRecipientName(body.recipient().fullName());
        command.setRelationship(body.recipient().relationship());
        command.setReturnedAt(body.returnedAt());
        familyMedicationService.returnBatch(id, command, currentUser.accountId());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/api/v1/family-medications/{id}/dispose")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Đã hủy theo yêu cầu gia đình", content = @Content),
        @ApiResponse(
                responseCode = "409",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class)))
    })
    public ResponseEntity<Void> disposeBatch(
            @PathVariable("id") Long id,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody DisposeRequest body) {
        DisposeBatchBody command = new DisposeBatchBody();
        command.setQuantity(body.quantity());
        command.setRepresentativeId(body.representativeId());
        command.setRequestMethod(body.requestMethod());
        command.setWitness(body.witness());
        command.setReason(body.reason());
        familyMedicationService.disposeBatch(id, command, currentUser.accountId());
        return ResponseEntity.ok().build();
    }
}
