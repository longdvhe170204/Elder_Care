package com.eldercare.backend.incident.web;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.incident.internal.emergency.domain.TransferOutCommand;
import com.eldercare.backend.incident.internal.emergency.service.EmergencyCardService;
import com.eldercare.backend.incident.internal.emergency.service.TransferOutCoordinator;
import com.eldercare.backend.incident.internal.emergency.service.TransferOutService;
import com.eldercare.backend.incident.internal.emergency.service.WishCheckService;
import com.eldercare.backend.incident.web.request.TransferOutBody;
import com.eldercare.backend.incident.web.response.ActivationResultResponse.ActivationEmergencyCard;
import com.eldercare.backend.incident.web.response.M05ProblemBody;
import com.eldercare.backend.incident.web.response.TransferOutInfoResponse;
import com.eldercare.backend.incident.web.response.TransferSummaryResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Quy trình khẩn cấp (tag {@code emergency} của api-007; US5, T042, T105): thẻ thông tin khẩn cấp, xác nhận đối chiếu
 * nguyện vọng, Chuyển viện từ sự cố, bản tóm tắt và phiếu PDF.
 */
@RestController
@RequestMapping("/api/v1")
public class EmergencyController {
    private static final String PROBLEM = "application/problem+json";

    private final EmergencyCardService cards;
    private final WishCheckService wishChecks;
    private final TransferOutCoordinator transferOut;
    private final TransferOutService transfers;
    private final CurrentUser currentUser;

    public EmergencyController(
            EmergencyCardService cards,
            WishCheckService wishChecks,
            TransferOutCoordinator transferOut,
            TransferOutService transfers,
            CurrentUser currentUser) {
        this.cards = cards;
        this.wishChecks = wishChecks;
        this.transferOut = transferOut;
        this.transfers = transfers;
        this.currentUser = currentUser;
    }

    @GetMapping("/incidents/{id}/emergency-card")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "OK")})
    public ActivationEmergencyCard card(@PathVariable long id) {
        return ActivationEmergencyCard.of(cards.card(id, currentUser));
    }

    @PostMapping("/incidents/{id}/confirm-wish-check")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "OK"),
        @ApiResponse(
                responseCode = "403",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = M05ProblemBody.class))),
        @ApiResponse(
                responseCode = "409",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = M05ProblemBody.class)))
    })
    public ResponseEntity<Void> confirmWishCheck(
            @PathVariable long id, @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        wishChecks.confirm(id, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/incidents/{id}/transfer-out")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Bản tóm tắt"),
        @ApiResponse(
                responseCode = "409",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = M05ProblemBody.class)))
    })
    public ResponseEntity<TransferSummaryResponse> transferOut(
            @PathVariable long id,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody TransferOutBody body) {
        long summaryId = transferOut.transfer(
                id,
                new TransferOutCommand(
                        body.transferUnit(),
                        body.transferAt(),
                        body.expectedReturnAt(),
                        body.handoverStaffId(),
                        body.companion(),
                        body.requiredContactReached(),
                        body.notAskedFirstReason() == null
                                ? null
                                : body.notAskedFirstReason().value(),
                        body.attachmentIds()),
                currentUser);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(TransferSummaryResponse.of(transfers.summary(summaryId, currentUser)));
    }

    @GetMapping("/incidents/{id}/transfer-out/info")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "OK")})
    public TransferOutInfoResponse transferOutInfo(@PathVariable long id) {
        return TransferOutInfoResponse.of(transfers.info(id, currentUser));
    }

    @GetMapping("/transfer-summaries/{id}")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "OK")})
    public TransferSummaryResponse summary(@PathVariable long id) {
        return TransferSummaryResponse.of(transfers.summary(id, currentUser));
    }

    @GetMapping("/transfer-summaries/{id}/document")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "PDF",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_PDF_VALUE,
                                schema = @Schema(type = "string", format = "binary"))),
        @ApiResponse(
                responseCode = "403",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = M05ProblemBody.class)))
    })
    public ResponseEntity<byte[]> document(@PathVariable long id) {
        return pdf(transfers.document(id, currentUser));
    }

    @PostMapping("/transfer-summaries/{id}/reprint")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "PDF",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_PDF_VALUE,
                                schema = @Schema(type = "string", format = "binary"))),
        @ApiResponse(
                responseCode = "403",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = M05ProblemBody.class)))
    })
    public ResponseEntity<byte[]> reprint(@PathVariable long id) {
        return pdf(transfers.reprint(id, currentUser));
    }

    private static ResponseEntity<byte[]> pdf(byte[] content) {
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF).body(content);
    }
}
