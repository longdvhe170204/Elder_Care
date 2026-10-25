package com.cambae.backend.m03.web;

import com.cambae.backend.common.data.CatalogStatus;
import com.cambae.backend.common.web.VersionEtag;
import com.cambae.backend.m03.internal.structure.domain.*;
import com.cambae.backend.m03.internal.structure.service.StructureQueries;
import com.cambae.backend.m03.internal.structure.service.StructureService;
import com.cambae.backend.m03.web.request.*;
import com.cambae.backend.m03.web.response.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** T017: cơ cấu theo hợp đồng Phase 3 đã duyệt, không lộ entity lưu trữ. */
@RestController
@RequestMapping("/api/v1/areas")
public class AreaController {
    private final StructureService service;
    private final StructureQueries queries;

    public AreaController(StructureService service, StructureQueries queries) {
        this.service = service;
        this.queries = queries;
    }

    @GetMapping
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "Thành công",
                content =
                        @Content(
                                mediaType = "application/json",
                                array = @ArraySchema(schema = @Schema(implementation = AreaResponse.class))))
    })
    public List<AreaResponse> list(@RequestParam(required = false) CatalogStatus status) {
        return service.list(StructureKind.AREA).stream()
                .filter(e -> status == null || e.getStatus() == status)
                .map(e -> AreaResponse.of((Area) e))
                .toList();
    }

    @PostMapping
    @Operation(
            parameters =
                    @Parameter(
                            name = "Idempotency-Key",
                            in = ParameterIn.HEADER,
                            required = true,
                            schema = @Schema(type = "string")))
    @ApiResponses({
        @ApiResponse(
                responseCode = "201",
                description = "Thành công",
                content =
                        @Content(
                                mediaType = "application/json",
                                schema = @Schema(implementation = AreaResponse.class))),
        @ApiResponse(
                responseCode = "403",
                description = "Lỗi nghiệp vụ",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = StructureProblem.class))),
        @ApiResponse(
                responseCode = "409",
                description = "Lỗi nghiệp vụ",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = StructureProblem.class)))
    })
    public ResponseEntity<AreaResponse> create(@Valid @RequestBody AreaWrite body) {
        var entity = service.create(StructureKind.AREA, body.change());
        return ResponseEntity.status(201)
                .eTag(VersionEtag.of(entity.getVersion()))
                .body(AreaResponse.of((Area) entity));
    }

    @PutMapping("/{id}")
    @Operation(
            parameters =
                    @Parameter(
                            name = "Idempotency-Key",
                            in = ParameterIn.HEADER,
                            required = true,
                            schema = @Schema(type = "string")))
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "Thành công",
                content =
                        @Content(
                                mediaType = "application/json",
                                schema = @Schema(implementation = AreaResponse.class))),
        @ApiResponse(
                responseCode = "412",
                description = "Lỗi nghiệp vụ",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = StructureProblem.class)))
    })
    public ResponseEntity<AreaResponse> update(
            @PathVariable long id, @RequestHeader("If-Match") String ifMatch, @Valid @RequestBody AreaWrite body) {
        var entity = service.update(StructureKind.AREA, id, ifMatch, body.change());
        return ResponseEntity.ok().eTag(VersionEtag.of(entity.getVersion())).body(AreaResponse.of((Area) entity));
    }

    @DeleteMapping("/{id}")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Thành công", content = @Content),
        @ApiResponse(
                responseCode = "409",
                description = "Lỗi nghiệp vụ",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = StructureProblem.class)))
    })
    public ResponseEntity<Void> delete(@PathVariable long id, @RequestHeader("If-Match") String ifMatch) {
        service.delete(StructureKind.AREA, id, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "Thành công",
                content =
                        @Content(mediaType = "application/json", schema = @Schema(implementation = AreaResponse.class)))
    })
    public ResponseEntity<AreaResponse> detail(@PathVariable long id) {
        var entity = service.detail(StructureKind.AREA, id);
        return ResponseEntity.ok().eTag(VersionEtag.of(entity.getVersion())).body(AreaResponse.of((Area) entity));
    }

    @PostMapping("/{id}/deactivate")
    @Operation(
            parameters =
                    @Parameter(
                            name = "Idempotency-Key",
                            in = ParameterIn.HEADER,
                            required = true,
                            schema = @Schema(type = "string")))
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "Thành công",
                content =
                        @Content(
                                mediaType = "application/json",
                                schema = @Schema(implementation = AreaResponse.class))),
        @ApiResponse(
                responseCode = "409",
                description = "Lỗi nghiệp vụ",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = StructureProblem.class)))
    })
    public ResponseEntity<AreaResponse> deactivate(
            @PathVariable long id, @RequestHeader("If-Match") String ifMatch, @Valid @RequestBody ReasonBody body) {
        var entity = service.deactivate(StructureKind.AREA, id, ifMatch, body.reason());
        return ResponseEntity.ok().eTag(VersionEtag.of(entity.getVersion())).body(AreaResponse.of((Area) entity));
    }

    @PostMapping("/{id}/reactivate")
    @Operation(
            parameters =
                    @Parameter(
                            name = "Idempotency-Key",
                            in = ParameterIn.HEADER,
                            required = true,
                            schema = @Schema(type = "string")))
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "Thành công",
                content =
                        @Content(mediaType = "application/json", schema = @Schema(implementation = AreaResponse.class)))
    })
    public ResponseEntity<AreaResponse> reactivate(
            @PathVariable long id, @RequestHeader("If-Match") String ifMatch, @Valid @RequestBody ReasonBody body) {
        var entity = service.reactivate(StructureKind.AREA, id, ifMatch, body.reason());
        return ResponseEntity.ok().eTag(VersionEtag.of(entity.getVersion())).body(AreaResponse.of((Area) entity));
    }
}
