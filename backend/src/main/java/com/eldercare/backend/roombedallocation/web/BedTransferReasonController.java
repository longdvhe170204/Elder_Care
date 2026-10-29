package com.eldercare.backend.roombedallocation.web;

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
@RequestMapping("/api/v1/bed-transfer-reasons")
public class BedTransferReasonController {
    private final StructureService service;
    private final StructureQueries queries;

    public BedTransferReasonController(StructureService service, StructureQueries queries) {
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
                                array =
                                        @ArraySchema(
                                                schema = @Schema(implementation = BedTransferReasonResponse.class))))
    })
    public List<BedTransferReasonResponse> list() {
        return service.list(StructureKind.BED_TRANSFER_REASON).stream()
                .map(e -> BedTransferReasonResponse.of((BedTransferReason) e))
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
                                schema = @Schema(implementation = BedTransferReasonResponse.class)))
    })
    public ResponseEntity<BedTransferReasonResponse> create(@Valid @RequestBody BedTransferReasonWrite body) {
        var entity = service.create(StructureKind.BED_TRANSFER_REASON, body.change());
        return ResponseEntity.status(201)
                .eTag(VersionEtag.of(entity.getVersion()))
                .body(BedTransferReasonResponse.of((BedTransferReason) entity));
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
            @PathVariable long id,
            @RequestHeader("If-Match") String ifMatch,
            @Valid @RequestBody BedTransferReasonWrite body) {
        var entity = service.update(StructureKind.BED_TRANSFER_REASON, id, ifMatch, body.change());
        return ResponseEntity.ok().eTag(VersionEtag.of(entity.getVersion())).build();
    }

    @GetMapping("/{id}")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "Thành công",
                content =
                        @Content(
                                mediaType = "application/json",
                                schema = @Schema(implementation = BedTransferReasonResponse.class)))
    })
    public ResponseEntity<BedTransferReasonResponse> detail(@PathVariable long id) {
        var entity = service.detail(StructureKind.BED_TRANSFER_REASON, id);
        return ResponseEntity.ok()
                .eTag(VersionEtag.of(entity.getVersion()))
                .body(BedTransferReasonResponse.of((BedTransferReason) entity));
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
        service.delete(StructureKind.BED_TRANSFER_REASON, id, ifMatch);
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
        var entity = service.deactivate(StructureKind.BED_TRANSFER_REASON, id, ifMatch, body.reason());
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
                                schema = @Schema(implementation = BedTransferReasonResponse.class))),
        @ApiResponse(
                responseCode = "409",
                description = "Lỗi nghiệp vụ",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = StructureProblem.class)))
    })
    public ResponseEntity<BedTransferReasonResponse> reactivate(
            @PathVariable long id, @RequestHeader("If-Match") String ifMatch, @Valid @RequestBody ReasonBody body) {
        var entity = service.reactivate(StructureKind.BED_TRANSFER_REASON, id, ifMatch, body.reason());
        return ResponseEntity.ok()
                .eTag(VersionEtag.of(entity.getVersion()))
                .body(BedTransferReasonResponse.of((BedTransferReason) entity));
    }
}
