package com.eldercare.backend.reporting.web;

import com.eldercare.backend.common.error.web.ProblemBody;
import com.eldercare.backend.reporting.internal.export.service.ReportExportService;
import com.eldercare.backend.reporting.internal.export.service.ReportExportService.ExportFormat;
import com.eldercare.backend.reporting.internal.export.service.ReportExportService.ExportRecord;
import com.eldercare.backend.reporting.internal.export.service.ReportExportService.ExportRequest;
import com.eldercare.backend.reporting.internal.export.service.ReportExportService.ExportResult;
import com.eldercare.backend.reporting.internal.report.domain.ReportGroup;
import com.eldercare.backend.reporting.web.request.ExportRequestBody;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Xuất báo cáo và lần xuất theo contracts/api-016.yaml (thẻ {@code export}; 016 T026). Tải tệp qua
 * {@code GET /files/{id}}.
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "export")
public class ReportExportController {

    private final ReportExportService exports;

    public ReportExportController(ReportExportService exports) {
        this.exports = exports;
    }

    @PostMapping("/reports/{group}/export")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Mã tệp để tải và mã nhật ký"),
        @ApiResponse(
                responseCode = "403",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = ProblemBody.class))),
        @ApiResponse(
                responseCode = "422",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = ProblemBody.class)))
    })
    ResponseEntity<ExportResult> export(
            @PathVariable ReportGroup group,
            @RequestHeader(name = "Idempotency-Key") UUID idempotencyKey,
            @Valid @RequestBody ExportRequestBody body) {
        ExportResult result = exports.export(new ExportRequest(
                group,
                body.from(),
                body.to(),
                body.toFilters(),
                Boolean.TRUE.equals(body.includeIdentity()),
                Optional.ofNullable(body.purpose()),
                body.format() == null ? ExportFormat.XLSX : body.format()));
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @GetMapping("/reports/exports")
    @ApiResponse(responseCode = "200", description = "OK")
    List<ExportRecord> list() {
        return exports.list();
    }
}
