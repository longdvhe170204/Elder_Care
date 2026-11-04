package com.eldercare.backend.incident.web;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.incident.internal.alert.service.AlertConversionService;
import com.eldercare.backend.incident.internal.incident.service.IncidentQueryService;
import com.eldercare.backend.incident.web.request.ConvertToIncidentRequest;
import com.eldercare.backend.incident.web.response.ActivationResultResponse.ActivationIncident;
import com.eldercare.backend.incident.web.response.M05ProblemBody;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** {@code POST /alerts/{id}/convert-to-incident}: Chuyển sự cố từ cảnh báo Đang xử lý (FR-029, FR-040; T037). */
@RestController
@RequestMapping("/api/v1/alerts")
public class AlertConversionController {
    private static final String PROBLEM = "application/problem+json";

    private final AlertConversionService conversion;
    private final IncidentQueryService queries;
    private final CurrentUser currentUser;

    public AlertConversionController(
            AlertConversionService conversion, IncidentQueryService queries, CurrentUser currentUser) {
        this.conversion = conversion;
        this.queries = queries;
        this.currentUser = currentUser;
    }

    @PostMapping("/{id}/convert-to-incident")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Sự cố đã tạo"),
        @ApiResponse(
                responseCode = "409",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = M05ProblemBody.class)))
    })
    public ResponseEntity<ActivationIncident> convert(
            @PathVariable long id,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody ConvertToIncidentRequest request) {
        long incidentId = conversion.convert(
                id,
                request.incidentTypeId(),
                request.description(),
                request.initialResponse(),
                request.residentId(),
                currentUser);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(IncidentQueryController.toResponse(queries.detail(incidentId, currentUser)));
    }
}
