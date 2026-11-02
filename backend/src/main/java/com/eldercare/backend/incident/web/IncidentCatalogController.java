package com.eldercare.backend.incident.web;

import com.eldercare.backend.common.web.VersionEtag;
import com.eldercare.backend.incident.internal.alert.service.IncidentCatalogService;
import com.eldercare.backend.incident.web.request.*;
import com.eldercare.backend.incident.web.response.*;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** HTTP danh mục cảnh báo và sự cố M05 theo FR-039, FR-042 và FR-048a. */
@RestController
@RequestMapping("/api/v1")
class IncidentCatalogController {

    private final IncidentCatalogService catalogs;

    IncidentCatalogController(IncidentCatalogService catalogs) {
        this.catalogs = catalogs;
    }

    @GetMapping("/trend-rules")
    List<TrendRuleResponse> trendRules() {
        return catalogs.trendRules().stream().map(TrendRuleResponse::from).toList();
    }

    @PutMapping("/trend-rules/{code}")
    ResponseEntity<Void> updateTrendRule(
            @PathVariable String code,
            @RequestHeader("If-Match") String ifMatch,
            @RequestBody TrendRuleRequest request) {
        var updated = catalogs.updateTrendRule(code, ifMatch, request.toInput(), request.reason());
        return ResponseEntity.ok().eTag(VersionEtag.of(updated.version())).build();
    }

    @GetMapping("/incident-types")
    List<IncidentTypeResponse> incidentTypes() {
        return catalogs.incidentTypes().stream().map(IncidentTypeResponse::from).toList();
    }

    @PostMapping("/incident-types")
    ResponseEntity<Void> createIncidentType(@RequestBody IncidentTypeRequest request) {
        var created = catalogs.createIncidentType(request.toInput());
        return ResponseEntity.status(HttpStatus.CREATED)
                .eTag(VersionEtag.of(created.version()))
                .build();
    }

    @PutMapping("/incident-types/{id}")
    ResponseEntity<Void> updateIncidentType(
            @PathVariable long id,
            @RequestHeader("If-Match") String ifMatch,
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
    ResponseEntity<Void> createProgressionType(@RequestBody ProgressionTypeRequest request) {
        var created = catalogs.createProgressionType(request.toInput());
        return ResponseEntity.status(HttpStatus.CREATED)
                .eTag(VersionEtag.of(created.version()))
                .build();
    }
}
