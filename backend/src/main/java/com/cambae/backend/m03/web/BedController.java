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
    private final com.cambae.backend.m15.account.api.AccountApi accounts;
    private final com.cambae.backend.m15.correction.api.CorrectionApi corrections;

    public BedController(
            BedService beds,
            BedQueries queries,
            com.cambae.backend.m15.account.api.AccountApi accounts,
            com.cambae.backend.m15.correction.api.CorrectionApi corrections) {
        this.beds = beds;
        this.queries = queries;
        this.accounts = accounts;
        this.corrections = corrections;
    }

    @PostMapping("/{id}/suspend")
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
                description = "Đã tạm ngừng",
                content =
                        @Content(mediaType = "application/json", schema = @Schema(implementation = BedResponse.class))),
        @ApiResponse(
                responseCode = "403",
                description = "Không có quyền",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = StructureProblem.class))),
        @ApiResponse(
                responseCode = "409",
                description = "Điều kiện chưa thỏa",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = StructureProblem.class))),
        @ApiResponse(
                responseCode = "422",
                description = "Thiếu lý do",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = StructureProblem.class)))
    })
    public BedResponse suspend(@PathVariable long id, @Valid @RequestBody ReasonBody body) {
        beds.suspend(id, body.reason());
        return BedResponse.of(queries.detail(id));
    }

    @PostMapping("/{id}/reuse")
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
                description = "Đã sử dụng lại",
                content =
                        @Content(mediaType = "application/json", schema = @Schema(implementation = BedResponse.class))),
        @ApiResponse(
                responseCode = "409",
                description = "Điều kiện chưa thỏa",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = StructureProblem.class)))
    })
    public BedResponse reuse(@PathVariable long id, @Valid @RequestBody ReasonBody body) {
        beds.reuse(id, body.reason());
        return BedResponse.of(queries.detail(id));
    }

    @GetMapping("/{id}/state-history")
    @ApiResponse(responseCode = "200", description = "Lịch sử gốc có dấu đính chính")
    public List<BedStateHistoryRow> history(@PathVariable long id, @RequestParam(required = false) String after) {
        var rows = queries.history(id, after);
        var current = corrections.current(
                "M03.BED_STATE_HISTORY",
                rows.stream()
                        .map(com.cambae.backend.m03.internal.bed.domain.BedHistoryEntry::id)
                        .toList());
        return rows.stream()
                .map(row -> new BedStateHistoryRow(
                        row.id(),
                        row.from(),
                        row.to(),
                        row.event(),
                        row.occurredAt(),
                        row.plannedAt(),
                        row.executedLate(),
                        row.actorId() == null
                                ? (row.plannedAt() == null ? null : "Bộ lập lịch hệ thống")
                                : accounts.fullName(row.actorId()).orElse(null),
                        row.reason(),
                        new BedStateHistoryRow.HistoryBasis(row.basisType(), row.basisId()),
                        row.disinfected(),
                        row.ppeUsed(),
                        row.note(),
                        current.get(row.id()) != null && current.get(row.id()).corrected()))
                .toList();
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
