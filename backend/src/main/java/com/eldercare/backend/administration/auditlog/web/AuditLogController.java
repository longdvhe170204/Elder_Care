package com.eldercare.backend.administration.auditlog.web;

import com.eldercare.backend.administration.auditlog.internal.service.AuditLogQueryService;
import com.eldercare.backend.common.error.web.ProblemBody;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** {@code /api/v1/audit-logs} theo contracts/api-000.yaml (000 T069; FR-049). */
@RestController
@RequestMapping("/api/v1/audit-logs")
public class AuditLogController {

    private final AuditLogQueryService queries;

    public AuditLogController(AuditLogQueryService queries) {
        this.queries = queries;
    }

    @GetMapping
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(
                responseCode = "403",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = ProblemBody.class)))
    })
    public AuditLogQueryService.Page search(
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String entityId,
            @RequestParam(required = false) Long actorId,
            @RequestParam(required = false) List<String> action,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            @RequestParam(required = false) Boolean selfApproved,
            @RequestParam(required = false) String cursor) {
        return queries.search(
                new AuditLogQueryService.Filter(
                        entityType,
                        entityId,
                        actorId,
                        action,
                        from == null ? null : from.toInstant(),
                        to == null ? null : to.toInstant(),
                        selfApproved),
                cursor);
    }

    @GetMapping("/entities/{entityType}/{entityId}")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(
                responseCode = "404",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = ProblemBody.class)))
    })
    public AuditLogQueryService.Page history(
            @PathVariable String entityType,
            @PathVariable String entityId,
            @RequestParam(required = false) String cursor) {
        return queries.history(entityType, entityId, cursor);
    }
}
