package com.eldercare.backend.incident.web;

import com.eldercare.backend.common.web.VersionEtag;
import com.eldercare.backend.incident.internal.alert.service.IncidentCatalogService;
import com.eldercare.backend.incident.web.request.*;
import com.eldercare.backend.incident.web.response.*;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** HTTP danh mục cảnh báo và sự cố M05 theo FR-039, FR-042 và FR-048a. */
@RestController
@RequestMapping("/api/v1")
class IncidentCatalogController {

    private static final String PROBLEM = "application/problem+json";

    private final IncidentCatalogService catalogs;

    IncidentCatalogController(IncidentCatalogService catalogs) {
        this.catalogs = catalogs;
    }

    @GetMapping("/trend-rules")
    List<TrendRuleResponse> trendRules() {
        return catalogs.trendRules().stream().map(TrendRuleResponse::from).toList();
    }

    @PutMapping("/trend-rules/{code}")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK", content = @Content),
        @ApiResponse(
                responseCode = "412",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = M05ProblemBody.class))),
        @ApiResponse(
                responseCode = "422",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = M05ProblemBody.class)))
    })
    ResponseEntity<Void> updateTrendRule(
            @PathVariable
                    @Schema(
                            allowableValues = {
                                "WEIGHT_LOSS",
                                "MISSED_MEASUREMENT",
                                "PROLONGED_INSOMNIA",
                                "MEDICATION_REFUSAL"
                            })
                    String code,
            @RequestHeader("If-Match") String ifMatch,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody TrendRuleRequest request) {
        var updated = catalogs.updateTrendRule(code, ifMatch, request.toInput(), request.reason());
        return ResponseEntity.ok().eTag(VersionEtag.of(updated.version())).build();
    }

    @GetMapping("/incident-types")
    List<IncidentTypeResponse> incidentTypes() {
        return catalogs.incidentTypes().stream().map(IncidentTypeResponse::from).toList();
    }

    @PostMapping("/incident-types")
    @ApiResponse(responseCode = "201", description = "Đã thêm", content = @Content)
    ResponseEntity<Void> createIncidentType(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody IncidentTypeRequest request) {
        var created = catalogs.createIncidentType(request.toInput());
        return ResponseEntity.status(HttpStatus.CREATED)
                .eTag(VersionEtag.of(created.version()))
                .build();
    }

    @PutMapping("/incident-types/{id}")
    ResponseEntity<Void> updateIncidentType(
            @PathVariable long id,
            @RequestHeader("If-Match") String ifMatch,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody IncidentTypeRequest request) {
        var updated = request.active() == null
                ? catalogs.updateIncidentType(id, ifMatch, request.toInput())
                : catalogs.updateIncidentType(id, ifMatch, request.toInput(), request.active());
        return ResponseEntity.ok().eTag(VersionEtag.of(updated.version())).build();
    }

    @GetMapping("/progression-types")
    List<ProgressionTypeResponse> progressionTypes() {
        return catalogs.progressionTypes().stream()
                .map(ProgressionTypeResponse::from)
                .toList();
    }

    @PostMapping("/progression-types")
    @ApiResponse(responseCode = "201", description = "Đã thêm", content = @Content)
    ResponseEntity<Void> createProgressionType(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody ProgressionTypeRequest request) {
        var created = catalogs.createProgressionType(request.toInput());
        return ResponseEntity.status(HttpStatus.CREATED)
                .eTag(VersionEtag.of(created.version()))
                .build();
    }
}
