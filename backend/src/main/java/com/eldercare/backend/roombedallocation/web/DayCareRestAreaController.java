package com.eldercare.backend.roombedallocation.web;

import com.eldercare.backend.administration.parameter.api.*;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.common.web.VersionEtag;
import com.eldercare.backend.roombedallocation.internal.daycare.service.*;
import com.eldercare.backend.roombedallocation.web.request.*;
import com.eldercare.backend.roombedallocation.web.response.*;
import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.responses.*;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** T052: năm operation khu nghỉ đã dịch và duyệt. */
@RestController
@RequestMapping("/api/v1/day-care-rest-areas")
public class DayCareRestAreaController {
    private final DayCareConfigurationService service;
    private final ParameterApi parameters;
    private final BusinessTime time;

    public DayCareRestAreaController(DayCareConfigurationService service, ParameterApi parameters, BusinessTime time) {
        this.service = service;
        this.parameters = parameters;
        this.time = time;
    }

    @GetMapping
    public List<DayCareRestAreaResponse> list() {
        var rows = service.areas();
        var value = parameters.read(ParameterCode.CFG_M03_01);
        Integer limit = value.isSet() ? value.asInt() : null;
        return rows.stream().map(x -> DayCareRestAreaResponse.of(x, limit)).toList();
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
        @ApiResponse(responseCode = "201", description = "Đã tạo", content = @Content),
        @ApiResponse(
                responseCode = "409",
                description = "Bị chặn",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = StructureProblem.class)))
    })
    public ResponseEntity<Void> create(@Valid @RequestBody DayCareRestAreaWrite body) {
        var value = service.createArea(body.code(), body.name(), body.floorId(), body.areaId());
        return ResponseEntity.status(201).eTag(VersionEtag.of(value.version())).build();
    }

    @PutMapping("/{id}")
    @Operation(
            parameters =
                    @Parameter(
                            name = "Idempotency-Key",
                            in = ParameterIn.HEADER,
                            required = true,
                            schema = @Schema(type = "string")))
    public ResponseEntity<Void> update(
            @PathVariable long id,
            @RequestHeader("If-Match") String etag,
            @Valid @RequestBody DayCareRestAreaWrite body) {
        var value = service.updateArea(id, etag, body.code(), body.name(), body.floorId(), body.areaId());
        return ResponseEntity.ok().eTag(VersionEtag.of(value.version())).build();
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
        @ApiResponse(responseCode = "200", description = "Đã ngừng", content = @Content),
        @ApiResponse(
                responseCode = "409",
                description = "Bị chặn",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = StructureProblem.class)))
    })
    public ResponseEntity<Void> deactivate(
            @PathVariable long id, @RequestHeader("If-Match") String etag, @RequestBody ReasonBody body) {
        var value = service.deactivateArea(id, etag, body.reason(), time.today());
        return ResponseEntity.ok().eTag(VersionEtag.of(value.version())).build();
    }

    @GetMapping("/registration-counts")
    public List<DayCareSessionCapacity> counts(@RequestParam LocalDate from, @RequestParam LocalDate to) {
        return service.counts(from, to).stream().map(DayCareSessionCapacity::of).toList();
    }
}
