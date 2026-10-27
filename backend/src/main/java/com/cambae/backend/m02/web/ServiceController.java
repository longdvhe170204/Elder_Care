package com.cambae.backend.m02.web;

import com.cambae.backend.common.error.web.ProblemBody;
import com.cambae.backend.common.web.VersionEtag;
import com.cambae.backend.m02.api.dto.ServiceType;
import com.cambae.backend.m02.internal.servicecatalog.service.ServiceCatalogService;
import com.cambae.backend.m02.web.request.PriceVersionInputBody;
import com.cambae.backend.m02.web.request.ReasonBody;
import com.cambae.backend.m02.web.request.ServiceInputBody;
import com.cambae.backend.m02.web.response.MismatchedPriceContractBody;
import com.cambae.backend.m02.web.response.PriceVersionBody;
import com.cambae.backend.m02.web.response.ServiceBody;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
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
 * Danh mục dịch vụ và phiên bản đơn giá (004 US9; contracts/api-004.yaml, tag {@code services}). Mọi logic và kiểm quyền ở
 * service; ETag của lệnh sửa là {@code version} của dịch vụ.
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "services")
class ServiceController {

    private static final String PROBLEM = "application/problem+json";

    private final ServiceCatalogService services;

    ServiceController(ServiceCatalogService services) {
        this.services = services;
    }

    @GetMapping("/services")
    List<ServiceBody> list(@RequestParam(name = "type", required = false) ServiceType type) {
        return services.list(ServiceCatalogService.target(null), type).stream()
                .map(ServiceBody::of)
                .toList();
    }

    @PostMapping("/services")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Đã tạo"),
        @ApiResponse(
                responseCode = "409",
                description = "SERVICE_NAME_EXISTS, CARE_SERVICE_LEVEL_EXISTS",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class)))
    })
    ResponseEntity<ServiceBody> create(
            @RequestHeader(name = "Idempotency-Key", required = false) UUID idempotencyKey,
            @Valid @RequestBody ServiceInputBody body) {
        var created = services.create(ServiceCatalogService.target(null), body.toInput());
        return ResponseEntity.status(HttpStatus.CREATED)
                .eTag(VersionEtag.of(created.version()))
                .body(ServiceBody.of(created));
    }

    @PutMapping("/services/{id}")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK", content = @Content),
        @ApiResponse(
                responseCode = "409",
                description = "SERVICE_INACTIVE, SERVICE_FIELD_LOCKED, SERVICE_NAME_EXISTS",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class))),
        @ApiResponse(
                responseCode = "412",
                description = "VERSION_MISMATCH",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class)))
    })
    ResponseEntity<Void> update(
            @PathVariable long id,
            @RequestHeader(name = "If-Match", required = false) String ifMatch,
            @RequestHeader(name = "Idempotency-Key", required = false) UUID idempotencyKey,
            @Valid @RequestBody ServiceInputBody body) {
        var updated = services.update(ServiceCatalogService.target(id), id, ifMatch, body.toInput());
        return ResponseEntity.ok().eTag(VersionEtag.of(updated.version())).build();
    }

    @PostMapping("/services/{id}/deactivate")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK", content = @Content),
        @ApiResponse(
                responseCode = "409",
                description = "SERVICE_ALREADY_INACTIVE",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class))),
        @ApiResponse(
                responseCode = "412",
                description = "VERSION_MISMATCH",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class)))
    })
    ResponseEntity<Void> deactivate(
            @PathVariable long id,
            @RequestHeader(name = "If-Match", required = false) String ifMatch,
            @RequestHeader(name = "Idempotency-Key", required = false) UUID idempotencyKey,
            @Valid @RequestBody ReasonBody body) {
        var updated = services.deactivate(ServiceCatalogService.target(id), id, ifMatch, body.reason());
        return ResponseEntity.ok().eTag(VersionEtag.of(updated.version())).build();
    }

    @GetMapping("/services/{id}/price-versions")
    List<PriceVersionBody> priceVersions(@PathVariable long id) {
        return services.priceVersions(ServiceCatalogService.target(id), id).stream()
                .map(PriceVersionBody::of)
                .toList();
    }

    @PostMapping("/services/{id}/price-versions")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Đã tạo"),
        @ApiResponse(
                responseCode = "409",
                description = "Chồng khoảng hiệu lực (DBR-08) hoặc rút ngắn phần đã áp dụng",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class)))
    })
    ResponseEntity<PriceVersionBody> createPriceVersion(
            @PathVariable long id,
            @RequestHeader(name = "Idempotency-Key", required = false) UUID idempotencyKey,
            @Valid @RequestBody PriceVersionInputBody body) {
        var created = services.createPriceVersion(
                ServiceCatalogService.target(id),
                id,
                java.math.BigDecimal.valueOf(body.unitPrice()),
                body.effectiveFrom(),
                body.effectiveTo());
        return ResponseEntity.status(HttpStatus.CREATED).body(PriceVersionBody.of(created));
    }

    @GetMapping("/services/{id}/mismatched-price-contracts")
    List<MismatchedPriceContractBody> mismatchedPriceContracts(@PathVariable long id) {
        return services.mismatchedContracts(ServiceCatalogService.target(id), id).stream()
                .map(MismatchedPriceContractBody::of)
                .toList();
    }
}
