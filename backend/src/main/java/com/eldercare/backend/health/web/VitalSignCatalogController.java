package com.eldercare.backend.health.web;

import com.eldercare.backend.common.web.VersionEtag;
import com.eldercare.backend.health.internal.catalog.service.VitalSignCatalogService;
import com.eldercare.backend.health.web.request.ReasonBody;
import com.eldercare.backend.health.web.request.VitalSignCatalogRequest;
import com.eldercare.backend.health.web.response.M06ProblemBody;
import com.eldercare.backend.health.web.response.VitalSignCatalogResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** HTTP danh mục chỉ số M06 theo FR-001 và FR-002. */
@RestController
@RequestMapping("/api/v1")
class VitalSignCatalogController {

    private static final String PROBLEM = "application/problem+json";

    private final VitalSignCatalogService catalogs;

    VitalSignCatalogController(VitalSignCatalogService catalogs) {
        this.catalogs = catalogs;
    }

    @GetMapping("/vital-sign-catalog")
    List<VitalSignCatalogResponse> list() {
        return catalogs.list().stream().map(VitalSignCatalogResponse::from).toList();
    }

    @PostMapping("/vital-sign-catalog")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Đã thêm", content = @Content),
        @ApiResponse(
                responseCode = "403",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = M06ProblemBody.class)))
    })
    ResponseEntity<Void> create(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody VitalSignCatalogRequest request) {
        var created = catalogs.create(request.toInput());
        return ResponseEntity.status(HttpStatus.CREATED)
                .eTag(VersionEtag.of(created.version()))
                .build();
    }

    @PutMapping("/vital-sign-catalog/{id}")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK", content = @Content),
        @ApiResponse(
                responseCode = "412",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = M06ProblemBody.class)))
    })
    ResponseEntity<Void> update(
            @PathVariable long id,
            @RequestHeader("If-Match") String ifMatch,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody VitalSignCatalogRequest request) {
        var updated = catalogs.update(id, ifMatch, request.toInput());
        return ResponseEntity.ok().eTag(VersionEtag.of(updated.version())).build();
    }

    @PostMapping("/vital-sign-catalog/{id}/deactivate")
    ResponseEntity<Void> deactivate(
            @PathVariable long id,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody ReasonBody body) {
        catalogs.deactivate(id, body.reason());
        return ResponseEntity.ok().build();
    }
}
