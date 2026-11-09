package com.eldercare.backend.billing.web;

import com.eldercare.backend.billing.api.FamilyPortalCostApi;
import com.eldercare.backend.billing.internal.export.service.CostAccountingExport;
import com.eldercare.backend.billing.internal.governance.service.CostAccess;
import com.eldercare.backend.billing.web.request.CostExportRequest;
import com.eldercare.backend.billing.web.response.CostProblemBody;
import com.eldercare.backend.billing.web.response.CostStatementBody;
import com.eldercare.backend.billing.web.response.ProvisionalCostSummaryBody;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
class CostAccessController {
    private final FamilyPortalCostApi portal;
    private final CostAccountingExport exports;
    private final CostAccess access;

    CostAccessController(FamilyPortalCostApi portal, CostAccountingExport exports, CostAccess access) {
        this.portal = portal;
        this.exports = exports;
        this.access = access;
    }

    @GetMapping("/residents/{residentId}/costs/statements")
    @ApiResponse(
            responseCode = "403",
            content =
                    @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = CostProblemBody.class)))
    @ApiResponse(
            responseCode = "200",
            content =
                    @Content(
                            mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = CostStatementBody.class))))
    List<CostStatementBody> statements(@PathVariable long residentId) {
        return portal.finalizedStatements(residentId, access.user()).stream()
                .map(CostStatementBody::of)
                .toList();
    }

    @GetMapping("/residents/{residentId}/costs/provisional")
    ProvisionalCostSummaryBody provisional(
            @PathVariable long residentId,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to) {
        return ProvisionalCostSummaryBody.of(portal.provisional(residentId, from, to, access.user()));
    }

    @PostMapping("/costs/periods/{id}/export")
    @ApiResponse(
            responseCode = "403",
            content =
                    @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = CostProblemBody.class)))
    @ApiResponse(
            responseCode = "200",
            content = {
                @Content(mediaType = "text/csv", schema = @Schema(type = "string")),
                @Content(
                        mediaType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                        schema = @Schema(type = "string", format = "binary"))
            })
    ResponseEntity<byte[]> export(
            @PathVariable long id,
            @RequestHeader("Idempotency-Key") UUID key,
            @Valid @RequestBody CostExportRequest body) {
        var file = exports.export(id, body.scope(), body.format());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.contentType()))
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(file.filename())
                                .build()
                                .toString())
                .body(file.bytes());
    }
}
