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
@RequestMapping("/api/v1/room-types")
public class RoomTypeController {
    private final StructureService service;
    private final StructureQueries queries;

    public RoomTypeController(StructureService service, StructureQueries queries) {
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
                                array = @ArraySchema(schema = @Schema(implementation = RoomTypeResponse.class))))
    })
    public List<RoomTypeResponse> list(@RequestParam(required = false) CatalogStatus status) {
        return service.list(StructureKind.ROOM_TYPE).stream()
                .filter(e -> status == null || e.getStatus() == status)
                .map(e -> RoomTypeResponse.of((RoomType) e))
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
                                schema = @Schema(implementation = RoomTypeResponse.class)))
    })
    public ResponseEntity<RoomTypeResponse> create(@Valid @RequestBody RoomTypeWrite body) {
        var entity = service.create(StructureKind.ROOM_TYPE, body.change());
        return ResponseEntity.status(201)
                .eTag(VersionEtag.of(entity.getVersion()))
                .body(RoomTypeResponse.of((RoomType) entity));
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
                                schema = @Schema(implementation = RoomTypeResponse.class)))
    })
    public ResponseEntity<RoomTypeResponse> update(
            @PathVariable long id, @RequestHeader("If-Match") String ifMatch, @Valid @RequestBody RoomTypeWrite body) {
        var entity = service.update(StructureKind.ROOM_TYPE, id, ifMatch, body.change());
        return ResponseEntity.ok().eTag(VersionEtag.of(entity.getVersion())).body(RoomTypeResponse.of((RoomType)
                entity));
    }

    @GetMapping("/{id}")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "Thành công",
                content =
                        @Content(
                                mediaType = "application/json",
                                schema = @Schema(implementation = RoomTypeResponse.class)))
    })
    public ResponseEntity<RoomTypeResponse> detail(@PathVariable long id) {
        var entity = service.detail(StructureKind.ROOM_TYPE, id);
        return ResponseEntity.ok().eTag(VersionEtag.of(entity.getVersion())).body(RoomTypeResponse.of((RoomType)
                entity));
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
        service.delete(StructureKind.ROOM_TYPE, id, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/deactivate")
    @Operation(
            parameters =
                    @Parameter(
                            name = "Idempotency-Key",
                            in = ParameterIn.HEADER,
                            required = true,
                            schema = @Schema(type = "string")))
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Thành công", content = @Content)})
    public ResponseEntity<Void> deactivate(
            @PathVariable long id, @RequestHeader("If-Match") String ifMatch, @Valid @RequestBody ReasonBody body) {
        var entity = service.deactivate(StructureKind.ROOM_TYPE, id, ifMatch, body.reason());
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
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "Thành công",
                content =
                        @Content(
                                mediaType = "application/json",
                                schema = @Schema(implementation = RoomTypeResponse.class))),
        @ApiResponse(
                responseCode = "409",
                description = "Lỗi nghiệp vụ",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = StructureProblem.class)))
    })
    public ResponseEntity<RoomTypeResponse> reactivate(
            @PathVariable long id, @RequestHeader("If-Match") String ifMatch, @Valid @RequestBody ReasonBody body) {
        var entity = service.reactivate(StructureKind.ROOM_TYPE, id, ifMatch, body.reason());
        return ResponseEntity.ok().eTag(VersionEtag.of(entity.getVersion())).body(RoomTypeResponse.of((RoomType)
                entity));
    }
}
