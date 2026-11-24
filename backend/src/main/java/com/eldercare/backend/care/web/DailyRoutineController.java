package com.eldercare.backend.care.web;

import com.eldercare.backend.care.internal.dailyschedule.service.DailyRoutineService;
import com.eldercare.backend.care.web.request.DailyRoutineWrite;
import com.eldercare.backend.care.web.response.CareProblemBody;
import com.eldercare.backend.care.web.response.DailyRoutineResponse;
import com.eldercare.backend.common.web.VersionEtag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** 005 FR-056 (Q-440): mốc sinh hoạt cố định của toàn viện; không có xóa. */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "danh-muc")
public class DailyRoutineController {
    private final DailyRoutineService service;

    public DailyRoutineController(DailyRoutineService service) {
        this.service = service;
    }

    @GetMapping("/daily-routines")
    public List<DailyRoutineResponse> list() {
        return service.list().stream().map(DailyRoutineResponse::of).toList();
    }

    @PostMapping("/daily-routines")
    @ApiResponse(responseCode = "201", description = "Đã tạo")
    @ApiResponse(
            responseCode = "409",
            content =
                    @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = CareProblemBody.class)))
    @Operation(
            parameters =
                    @Parameter(
                            name = "Idempotency-Key",
                            in = ParameterIn.HEADER,
                            required = true,
                            schema = @Schema(type = "string")))
    public ResponseEntity<Void> create(
            @RequestHeader("Idempotency-Key") String key, @RequestBody DailyRoutineWrite body) {
        var row = service.create(body.name(), body.time());
        return ResponseEntity.status(201).eTag(VersionEtag.of(row.version())).build();
    }

    @PutMapping("/daily-routines/{id}")
    @ApiResponse(responseCode = "200", description = "OK")
    @ApiResponse(
            responseCode = "412",
            content =
                    @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = CareProblemBody.class)))
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
            @RequestHeader("Idempotency-Key") String key,
            @RequestBody DailyRoutineWrite body) {
        var row = service.update(id, etag, body.name(), body.time());
        return ResponseEntity.ok().eTag(VersionEtag.of(row.version())).build();
    }

    @PostMapping("/daily-routines/{id}/deactivate")
    @ApiResponse(responseCode = "200", description = "OK")
    @ApiResponse(
            responseCode = "412",
            content =
                    @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = CareProblemBody.class)))
    @Operation(
            parameters =
                    @Parameter(
                            name = "Idempotency-Key",
                            in = ParameterIn.HEADER,
                            required = true,
                            schema = @Schema(type = "string")))
    public ResponseEntity<Void> deactivate(
            @PathVariable long id,
            @RequestHeader("If-Match") String etag,
            @RequestHeader("Idempotency-Key") String key) {
        service.deactivate(id, etag);
        return ResponseEntity.ok().build();
    }
}
