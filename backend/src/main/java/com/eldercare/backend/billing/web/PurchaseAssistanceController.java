package com.eldercare.backend.billing.web;

import com.eldercare.backend.billing.internal.purchase.service.*;
import com.eldercare.backend.billing.web.request.*;
import com.eldercare.backend.billing.web.request.CostWorkflowRequests.ReasonBody;
import com.eldercare.backend.billing.web.response.CostProblemBody;
import com.eldercare.backend.billing.web.response.PurchaseRequestBody;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.responses.*;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/purchase-assistance-requests")
class PurchaseAssistanceController {
    private final PurchaseAssistanceService service;
    private final PurchaseRecordingService recording;

    PurchaseAssistanceController(PurchaseAssistanceService service, PurchaseRecordingService recording) {
        this.service = service;
        this.recording = recording;
    }

    @GetMapping
    List<PurchaseRequestBody> list(
            @RequestParam(required = false) Long residentId, @RequestParam(required = false) String status) {
        return service.list(residentId, status).stream()
                .map(row -> PurchaseRequestBody.of(row, service.decisions(row.id())))
                .toList();
    }

    @PostMapping
    @ApiResponse(responseCode = "201", content = @Content)
    ResponseEntity<Void> create(
            @RequestHeader("Idempotency-Key") UUID key, @Valid @RequestBody NewPurchaseRequest body) {
        service.create(
                body.residentId(),
                body.catalogItemId(),
                body.description(),
                body.quantity(),
                body.estimatedAmount(),
                body.requester().type(),
                body.requester().id(),
                body.reason());
        return ResponseEntity.status(201).build();
    }

    @PostMapping("/{id}/send")
    void send(@PathVariable long id, @RequestHeader("Idempotency-Key") UUID key) {
        service.send(id);
    }

    @PostMapping("/{id}/agree")
    @ApiResponses({
        @ApiResponse(responseCode = "200", content = @Content),
        @ApiResponse(
                responseCode = "403",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = CostProblemBody.class))),
        @ApiResponse(
                responseCode = "409",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = CostProblemBody.class)))
    })
    void agree(@PathVariable long id, @RequestHeader("Idempotency-Key") UUID key) {
        service.agree(id, service.target(id));
    }

    @PostMapping("/{id}/reject")
    @ApiResponses({
        @ApiResponse(responseCode = "200", content = @Content),
        @ApiResponse(
                responseCode = "409",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = CostProblemBody.class)))
    })
    void reject(
            @PathVariable long id, @RequestHeader("Idempotency-Key") UUID key, @Valid @RequestBody ReasonBody body) {
        service.reject(id, body.reason(), service.target(id));
    }

    @PostMapping("/{id}/request-additional-consent")
    void additional(
            @PathVariable long id,
            @RequestHeader("Idempotency-Key") UUID key,
            @Valid @RequestBody PurchaseAdditionalRequest body) {
        service.requestAdditional(id, body.newAmount(), body.reason());
    }

    @PostMapping("/{id}/withdraw-additional-consent")
    void withdraw(
            @PathVariable long id, @RequestHeader("Idempotency-Key") UUID key, @Valid @RequestBody ReasonBody body) {
        service.withdraw(id, body.reason());
    }

    @PostMapping("/{id}/cancel")
    void cancel(
            @PathVariable long id, @RequestHeader("Idempotency-Key") UUID key, @Valid @RequestBody ReasonBody body) {
        service.cancel(id, body.reason());
    }

    @PostMapping("/{id}/record-purchased")
    @ApiResponses({
        @ApiResponse(responseCode = "200", content = @Content),
        @ApiResponse(
                responseCode = "409",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = CostProblemBody.class)))
    })
    void purchased(
            @PathVariable long id,
            @RequestHeader("Idempotency-Key") UUID key,
            @Valid @RequestBody RecordPurchasedRequest body) {
        recording.record(
                id,
                body.actualAmount(),
                body.purchasedOn(),
                body.receiptFileId(),
                body.urgentPurchase(),
                body.urgentPurchaseRequesterId());
    }
}
