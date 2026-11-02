package com.eldercare.backend.roombedallocation.web;

import com.eldercare.backend.common.web.VersionEtag;
import com.eldercare.backend.roombedallocation.internal.daycare.service.DayCareConfigurationService;
import com.eldercare.backend.roombedallocation.web.request.*;
import com.eldercare.backend.roombedallocation.web.response.*;
import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.responses.*;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** T052: bốn operation danh mục buổi, quyền ở service. */
@RestController
@RequestMapping("/api/v1/day-care-sessions")
public class DayCareSessionController {
    private final DayCareConfigurationService service;

    public DayCareSessionController(DayCareConfigurationService service) {
        this.service = service;
    }

    @GetMapping
    public List<DayCareSessionResponse> list() {
        return service.sessions().stream().map(DayCareSessionResponse::of).toList();
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
                description = "Đã tạo",
                content =
                        @Content(
                                mediaType = "application/json",
                                schema = @Schema(implementation = DayCareSessionChangeResponse.class))),
        @ApiResponse(
                responseCode = "409",
                description = "Bị chặn",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = StructureProblem.class)))
    })
    public ResponseEntity<DayCareSessionChangeResponse> create(@Valid @RequestBody DayCareSessionWrite body) {
        var value = service.createSession(body.name(), body.start(), body.end());
        return ResponseEntity.status(201)
                .eTag(VersionEtag.of(value.session().version()))
                .body(DayCareSessionChangeResponse.of(value));
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
                description = "Đã lưu",
                content =
                        @Content(
                                mediaType = "application/json",
                                schema = @Schema(implementation = DayCareSessionChangeResponse.class))),
        @ApiResponse(
                responseCode = "409",
                description = "Bị chặn",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = StructureProblem.class)))
    })
    public ResponseEntity<DayCareSessionChangeResponse> update(
            @PathVariable long id,
            @RequestHeader("If-Match") String etag,
            @Valid @RequestBody DayCareSessionWrite body) {
        var value = service.updateSession(id, etag, body.name(), body.start(), body.end());
        return ResponseEntity.ok()
                .eTag(VersionEtag.of(value.session().version()))
                .body(DayCareSessionChangeResponse.of(value));
    }

    @PostMapping("/{id}/deactivate")
    @Operation(
            parameters =
                    @Parameter(
                            name = "Idempotency-Key",
                            in = ParameterIn.HEADER,
                            required = true,
                            schema = @Schema(type = "string")))
    public ResponseEntity<Void> deactivate(
            @PathVariable long id, @RequestHeader("If-Match") String etag, @RequestBody ReasonBody body) {
        var value = service.deactivateSession(id, etag, body.reason());
        return ResponseEntity.ok().eTag(VersionEtag.of(value.version())).build();
    }
}
