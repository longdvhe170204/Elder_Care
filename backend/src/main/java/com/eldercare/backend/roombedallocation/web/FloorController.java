package com.eldercare.backend.roombedallocation.web;

import com.eldercare.backend.common.data.CatalogStatus;
import com.eldercare.backend.common.web.VersionEtag;
import com.eldercare.backend.roombedallocation.internal.structure.domain.*;
import com.eldercare.backend.roombedallocation.internal.structure.service.StructureQueries;
import com.eldercare.backend.roombedallocation.internal.structure.service.StructureService;
import com.eldercare.backend.roombedallocation.web.request.*;
import com.eldercare.backend.roombedallocation.web.response.*;
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
@RequestMapping("/api/v1/floors")
public class FloorController {
    private final StructureService service;
    private final StructureQueries queries;

    public FloorController(StructureService service, StructureQueries queries) {
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
                                array = @ArraySchema(schema = @Schema(implementation = FloorResponse.class))))
    })
    public List<FloorResponse> list(
            @RequestParam(required = false) Long areaId, @RequestParam(required = false) CatalogStatus status) {
        return service.list(StructureKind.FLOOR).stream()
                .filter(e -> areaId == null || ((Floor) e).areaId() == areaId)
                .filter(e -> status == null || e.getStatus() == status)
                .map(e -> FloorResponse.of((Floor) e))
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
                                schema = @Schema(implementation = FloorResponse.class))),
        @ApiResponse(
                responseCode = "409",
                description = "Lỗi nghiệp vụ",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = StructureProblem.class)))
    })
    public ResponseEntity<FloorResponse> create(@Valid @RequestBody FloorWrite body) {
        var entity = service.create(StructureKind.FLOOR, body.change());
        return ResponseEntity.status(201)
                .eTag(VersionEtag.of(entity.getVersion()))
                .body(FloorResponse.of((Floor) entity));
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
                                schema = @Schema(implementation = FloorResponse.class))),
        @ApiResponse(
                responseCode = "409",
                description = "Lỗi nghiệp vụ",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = StructureProblem.class)))
    })
    public ResponseEntity<FloorResponse> update(
            @PathVariable long id, @RequestHeader("If-Match") String ifMatch, @Valid @RequestBody FloorWrite body) {
        var entity = service.update(StructureKind.FLOOR, id, ifMatch, body.change());
        return ResponseEntity.ok().eTag(VersionEtag.of(entity.getVersion())).body(FloorResponse.of((Floor) entity));
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
        service.delete(StructureKind.FLOOR, id, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "Thành công",
                content =
                        @Content(
                                mediaType = "application/json",
                                schema = @Schema(implementation = FloorResponse.class)))
    })
    public ResponseEntity<FloorResponse> detail(@PathVariable long id) {
        var entity = service.detail(StructureKind.FLOOR, id);
        return ResponseEntity.ok().eTag(VersionEtag.of(entity.getVersion())).body(FloorResponse.of((Floor) entity));
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
        @ApiResponse(responseCode = "200", description = "Thành công", content = @Content),
        @ApiResponse(
                responseCode = "409",
                description = "Lỗi nghiệp vụ",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = StructureProblem.class)))
    })
    public ResponseEntity<Void> deactivate(
            @PathVariable long id, @RequestHeader("If-Match") String ifMatch, @Valid @RequestBody ReasonBody body) {
        var entity = service.deactivate(StructureKind.FLOOR, id, ifMatch, body.reason());
        return ResponseEntity.ok().eTag(VersionEtag.of(entity.getVersion())).build();
    }

    @PostMapping("/{id}/reactivate")
    @Operation(
            parameters =
                    @Parameter(
                            name = "Idempotency-Key",
                            in = ParameterIn.HEADER,
                            required = true,
                            schema = @Schema(type = "string")))
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Thành công", content = @Content)})
    public ResponseEntity<Void> reactivate(
            @PathVariable long id, @RequestHeader("If-Match") String ifMatch, @Valid @RequestBody ReasonBody body) {
        var entity = service.reactivate(StructureKind.FLOOR, id, ifMatch, body.reason());
        return ResponseEntity.ok().eTag(VersionEtag.of(entity.getVersion())).build();
    }
}
