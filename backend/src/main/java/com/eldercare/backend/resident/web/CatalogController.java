package com.eldercare.backend.resident.web;

import com.eldercare.backend.common.data.CatalogStatus;
import com.eldercare.backend.common.error.web.ProblemBody;
import com.eldercare.backend.common.web.VersionEtag;
import com.eldercare.backend.resident.api.dto.AllergenGroup;
import com.eldercare.backend.resident.internal.health.domain.CatalogItemView;
import com.eldercare.backend.resident.internal.health.service.CatalogService;
import com.eldercare.backend.resident.web.request.ReasonBody;
import com.eldercare.backend.resident.web.response.CatalogItemBody;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Danh mục của M01: {@code allergens}, {@code allergy-severities}, {@code information-sources}, {@code care-needs},
 * {@code care-levels} (cố định, chỉ đọc) (001 T012; contracts/api-001.yaml, tag {@code catalogs}).
 */
@RestController
@RequestMapping("/api/v1/catalogs/{type}")
class CatalogController {

    private static final String PROBLEM = "application/problem+json";

    private final CatalogService catalogs;

    CatalogController(CatalogService catalogs) {
        this.catalogs = catalogs;
    }

    @GetMapping
    @Tag(name = "catalogs")
    List<CatalogItemBody> list(
            @PathVariable String type,
            @Parameter(description = "Chỉ với allergens") @RequestParam(required = false) AllergenGroup group,
            @RequestParam(required = false) CatalogStatus status) {
        return catalogs.list(type, group == null ? null : group.name(), Optional.ofNullable(status)).stream()
                .map(CatalogItemBody::of)
                .toList();
    }

    @PostMapping
    @Tag(name = "catalogs")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "OK"),
        @ApiResponse(
                responseCode = "409",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class)))
    })
    ResponseEntity<CatalogItemBody> create(@PathVariable String type, @Valid @RequestBody CatalogItemBody body) {
        CatalogItemView created = catalogs.create(type, body.input());
        return ResponseEntity.status(HttpStatus.CREATED)
                .eTag(VersionEtag.of(created.version()))
                .body(CatalogItemBody.of(created));
    }

    @PutMapping("/{id}")
    @Tag(name = "catalogs")
    ResponseEntity<CatalogItemBody> update(
            @PathVariable String type,
            @PathVariable long id,
            @RequestHeader("If-Match") String ifMatch,
            @Valid @RequestBody CatalogItemBody body) {
        CatalogItemView updated = catalogs.update(type, id, ifMatch, body.input());
        return ResponseEntity.ok().eTag(VersionEtag.of(updated.version())).body(CatalogItemBody.of(updated));
    }

    @PostMapping("/{id}/deactivate")
    @Tag(name = "catalogs")
    ResponseEntity<Void> deactivate(@PathVariable String type, @PathVariable long id, @RequestBody ReasonBody body) {
        catalogs.deactivate(type, id, body.reason());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/reactivate")
    @Tag(name = "catalogs")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(
                responseCode = "409",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class)))
    })
    ResponseEntity<Void> reactivate(@PathVariable String type, @PathVariable long id, @RequestBody ReasonBody body) {
        catalogs.reactivate(type, id, body.reason());
        return ResponseEntity.ok().build();
    }
}
