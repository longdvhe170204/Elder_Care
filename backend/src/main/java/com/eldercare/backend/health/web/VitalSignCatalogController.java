package com.eldercare.backend.health.web;

import com.eldercare.backend.common.web.VersionEtag;
import com.eldercare.backend.health.internal.catalog.service.VitalSignCatalogService;
import com.eldercare.backend.health.web.request.ReasonBody;
import com.eldercare.backend.health.web.request.VitalSignCatalogRequest;
import com.eldercare.backend.health.web.response.VitalSignCatalogResponse;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** HTTP danh mục chỉ số M06 theo FR-001 và FR-002. */
@RestController
@RequestMapping("/api/v1")
class VitalSignCatalogController {

    private final VitalSignCatalogService catalogs;

    VitalSignCatalogController(VitalSignCatalogService catalogs) {
        this.catalogs = catalogs;
    }

    @GetMapping("/vital-sign-catalog")
    List<VitalSignCatalogResponse> list() {
        return catalogs.list().stream().map(VitalSignCatalogResponse::from).toList();
    }

    @PostMapping("/vital-sign-catalog")
    ResponseEntity<Void> create(@RequestBody VitalSignCatalogRequest request) {
        var created = catalogs.create(request.toInput());
        return ResponseEntity.status(HttpStatus.CREATED)
                .eTag(VersionEtag.of(created.version()))
                .build();
    }

    @PutMapping("/vital-sign-catalog/{id}")
    ResponseEntity<Void> update(
            @PathVariable long id,
            @RequestHeader("If-Match") String ifMatch,
            @RequestBody VitalSignCatalogRequest request) {
        var updated = catalogs.update(id, ifMatch, request.toInput());
        return ResponseEntity.ok().eTag(VersionEtag.of(updated.version())).build();
    }

    @PostMapping("/vital-sign-catalog/{id}/deactivate")
    ResponseEntity<Void> deactivate(@PathVariable long id, @RequestBody ReasonBody body) {
        catalogs.deactivate(id, body.reason());
        return ResponseEntity.ok().build();
    }
}
