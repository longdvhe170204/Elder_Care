package com.eldercare.backend.billing.balance.web;

import com.eldercare.backend.billing.api.dto.BalanceDocumentType;
import com.eldercare.backend.billing.balance.internal.document.service.FamilyCostsRight;
import com.eldercare.backend.billing.balance.internal.document.service.PaymentNoticeService;
import com.eldercare.backend.billing.balance.internal.ledger.service.BalanceQueries;
import com.eldercare.backend.billing.balance.internal.settlement.service.SettlementService;
import com.eldercare.backend.billing.balance.web.response.BalanceDocumentResponse;
import com.eldercare.backend.billing.balance.web.response.PaymentNoticeResponse;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.error.web.ProblemBody;
import com.eldercare.backend.common.file.FileContent;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Phiếu báo phí và chứng từ cổng người thân (017 T035; api-017 tag {@code payment-notices}, {@code family-portal};
 * FR-031, FR-037, FR-038; BR-M11-23).
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "payment-notices")
class PaymentNoticeController {

    private static final String PROBLEM = "application/problem+json";
    private static final MediaType XLSX =
            MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final PaymentNoticeService service;
    private final SettlementService settlements;
    private final FamilyCostsRight familyRight;

    PaymentNoticeController(PaymentNoticeService service, SettlementService settlements, FamilyCostsRight familyRight) {
        this.familyRight = familyRight;
        this.service = service;
        this.settlements = settlements;
    }

    @GetMapping("/residents/{residentId}/payment-notices")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "OK")})
    List<PaymentNoticeResponse> notices(@PathVariable long residentId) {
        return service.list(BalanceQueries.target(residentId), residentId).stream()
                .map(PaymentNoticeResponse::of)
                .toList();
    }

    @GetMapping("/payment-notices/{id}/file")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "File",
                content = {
                    @Content(
                            mediaType = MediaType.APPLICATION_PDF_VALUE,
                            schema = @Schema(type = "string", format = "binary")),
                    @Content(
                            mediaType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                            schema = @Schema(type = "string", format = "binary"))
                }),
        @ApiResponse(
                responseCode = "403",
                description = "PERMISSION_DENIED",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class)))
    })
    ResponseEntity<byte[]> file(
            @PathVariable long id,
            @Parameter(
                            schema =
                                    @Schema(
                                            allowableValues = {"PDF", "XLSX"},
                                            defaultValue = "PDF"))
                    @RequestParam(defaultValue = "PDF")
                    String format) {
        return body(service.download(BalanceQueries.target(service.residentOf(id)), id, xlsx(format)), xlsx(format));
    }

    @GetMapping("/family-portal/residents/{residentId}/documents")
    @Tag(name = "family-portal")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(
                responseCode = "403",
                description = "PERMISSION_DENIED",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class)))
    })
    List<BalanceDocumentResponse> documents(@PathVariable long residentId) {
        return service.portalDocuments(BalanceQueries.target(residentId), residentId).stream()
                .map(BalanceDocumentResponse::of)
                .toList();
    }

    @GetMapping("/family-portal/documents/{type}/{id}/file")
    @Tag(name = "family-portal")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "File",
                content = {
                    @Content(
                            mediaType = MediaType.APPLICATION_PDF_VALUE,
                            schema = @Schema(type = "string", format = "binary")),
                    @Content(
                            mediaType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                            schema = @Schema(type = "string", format = "binary"))
                }),
        @ApiResponse(
                responseCode = "403",
                description = "PERMISSION_DENIED",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class)))
    })
    ResponseEntity<byte[]> portalFile(
            @PathVariable BalanceDocumentType type,
            @PathVariable long id,
            @Parameter(
                            schema =
                                    @Schema(
                                            allowableValues = {"PDF", "XLSX"},
                                            defaultValue = "PDF"))
                    @RequestParam(defaultValue = "PDF")
                    String format) {
        boolean xlsx = xlsx(format);
        var target = BalanceQueries.target(service.residentOfDocument(type, id));
        if (type == BalanceDocumentType.PAYMENT_NOTICE) {
            return body(service.download(target, id, xlsx), xlsx);
        }
        if (xlsx) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST, "FORMAT_NOT_AVAILABLE", "Bảng quyết toán chỉ có tệp PDF");
        }
        familyRight.require(target.residentId());
        return body(settlements.download(target, id), false);
    }

    private static boolean xlsx(String format) {
        if ("XLSX".equals(format)) {
            return true;
        }
        if ("PDF".equals(format)) {
            return false;
        }
        throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_VALUE", "format là PDF hoặc XLSX");
    }

    private static ResponseEntity<byte[]> body(FileContent file, boolean xlsx) {
        return ResponseEntity.ok()
                .contentType(xlsx ? XLSX : MediaType.APPLICATION_PDF)
                .body(file.content());
    }
}
