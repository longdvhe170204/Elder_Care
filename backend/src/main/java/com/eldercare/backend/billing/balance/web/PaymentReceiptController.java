package com.eldercare.backend.billing.balance.web;

import com.eldercare.backend.billing.balance.internal.document.service.PaymentReceiptService;
import com.eldercare.backend.billing.balance.internal.ledger.service.BalanceQueries;
import com.eldercare.backend.common.error.web.ProblemBody;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Biên nhận nộp tiền PDF (Q-451; api-017 tag {@code payments}). Quyền kiểm ở service. */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "payments")
class PaymentReceiptController {

    private static final String PROBLEM = "application/problem+json";

    private final PaymentReceiptService receipts;

    PaymentReceiptController(PaymentReceiptService receipts) {
        this.receipts = receipts;
    }

    @GetMapping("/balance-transactions/{id}/receipt")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "File",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_PDF_VALUE,
                                schema = @Schema(type = "string", format = "binary"))),
        @ApiResponse(
                responseCode = "403",
                description = "PERMISSION_DENIED",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class))),
        @ApiResponse(
                responseCode = "404",
                description = "Không có giao dịch",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class))),
        @ApiResponse(
                responseCode = "409",
                description = "RECEIPT_NOT_AVAILABLE",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class)))
    })
    ResponseEntity<byte[]> receipt(@PathVariable long id) {
        return pdf(id);
    }

    @GetMapping("/family-portal/balance-transactions/{id}/receipt")
    @Tag(name = "family-portal")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "File",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_PDF_VALUE,
                                schema = @Schema(type = "string", format = "binary"))),
        @ApiResponse(
                responseCode = "403",
                description = "PERMISSION_DENIED",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class))),
        @ApiResponse(
                responseCode = "404",
                description = "Không có giao dịch",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class))),
        @ApiResponse(
                responseCode = "409",
                description = "RECEIPT_NOT_AVAILABLE",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class)))
    })
    ResponseEntity<byte[]> portalReceipt(@PathVariable long id) {
        return pdf(id);
    }

    @GetMapping("/residents/{residentId}/balance-transactions/summary")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "File",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_PDF_VALUE,
                                schema = @Schema(type = "string", format = "binary"))),
        @ApiResponse(
                responseCode = "403",
                description = "PERMISSION_DENIED",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class))),
        @ApiResponse(
                responseCode = "422",
                description = "Giao dịch không hợp lệ",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class)))
    })
    ResponseEntity<byte[]> summary(@PathVariable long residentId, @RequestParam("id") List<Long> ids) {
        return file(receipts.summary(BalanceQueries.target(residentId), residentId, ids));
    }

    private ResponseEntity<byte[]> pdf(long id) {
        return file(receipts.receipt(BalanceQueries.target(receipts.residentOf(id)), id));
    }

    private static ResponseEntity<byte[]> file(PaymentReceiptService.Receipt receipt) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(receipt.number() + ".pdf")
                                .build()
                                .toString())
                .body(receipt.content());
    }
}
