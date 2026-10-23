package com.cambae.backend.m03.web;

import com.cambae.backend.common.web.VersionEtag;
import com.cambae.backend.m03.api.dto.BedStatus;
import com.cambae.backend.m03.internal.bed.service.BedQueries;
import com.cambae.backend.m03.internal.bed.service.BedService;
import com.cambae.backend.m03.internal.structure.domain.*;
import com.cambae.backend.m03.web.request.*;
import com.cambae.backend.m03.web.response.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** T017: bảng/chi tiết/tạo/sửa mã giường; không có lệnh xóa hay đặt state. */
@RestController
@RequestMapping("/api/v1/beds")
public class BedController {
    private final BedService beds;
    private final BedQueries queries;

    public BedController(BedService beds, BedQueries queries) {
        this.beds = beds;
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
                                schema = @Schema(implementation = BedBoardResponse.class))),
        @ApiResponse(
                responseCode = "403",
                description = "Lỗi nghiệp vụ",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = StructureProblem.class)))
    })
    public BedBoardResponse list(
            @RequestParam(required = false) Long areaId,
            @RequestParam(required = false) Long floorId,
            @RequestParam(required = false) Long roomId,
            @RequestParam(required = false) List<BedStatus> status) {
        return new BedBoardResponse(
                queries.list(areaId, floorId, roomId, status).stream()
                        .map(BedResponse::of)
                        .toList(),
                List.of());
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
                                schema = @Schema(implementation = BedCreationResponse.class))),
        @ApiResponse(
                responseCode = "409",
                description = "Lỗi nghiệp vụ",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = StructureProblem.class)))
    })
    public ResponseEntity<BedCreationResponse> create(@Valid @RequestBody BedWrite body) {
        var result = beds.create(body.roomId(), body.code());
        return ResponseEntity.status(201)
                .eTag(VersionEtag.of(result.bed().version()))
                .body(new BedCreationResponse(
                        BedResponse.of(queries.detail(result.bed().id())),
                        result.warnings().stream()
                                .map(w -> new WarningResponse(w.code(), w.description()))
                                .toList()));
    }

    @GetMapping("/{id}")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "Thành công",
                content =
                        @Content(mediaType = "application/json", schema = @Schema(implementation = BedResponse.class)))
    })
    public ResponseEntity<BedResponse> detail(@PathVariable long id) {
        var d = queries.detail(id);
        return ResponseEntity.ok().eTag(VersionEtag.of(d.version())).body(BedResponse.of(d));
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
        @ApiResponse(responseCode = "200", description = "Thành công", content = @Content),
        @ApiResponse(
                responseCode = "409",
                description = "Lỗi nghiệp vụ",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = StructureProblem.class)))
    })
    public ResponseEntity<Void> update(
            @PathVariable long id, @RequestHeader("If-Match") String ifMatch, @Valid @RequestBody BedWrite body) {
        var bed = beds.update(id, ifMatch, body.roomId(), body.code());
        return ResponseEntity.ok().eTag(VersionEtag.of(bed.version())).build();
    }
}
