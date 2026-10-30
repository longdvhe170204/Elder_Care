package com.eldercare.backend.care.web;

import com.eldercare.backend.care.internal.catalog.service.NonPerformanceReasonService;
import com.eldercare.backend.care.web.request.CareCatalogReason;
import com.eldercare.backend.care.web.request.NonPerformanceReasonWrite;
import com.eldercare.backend.care.web.response.NonPerformanceReasonTypeResponse;
import com.eldercare.backend.common.error.web.ProblemBody;
import com.eldercare.backend.common.web.VersionEtag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** T007a: chỉ bốn operation danh mục đã được duyệt, không thêm xóa/kích hoạt lại/đọc chi tiết. */
@RestController
@RequestMapping("/api/v1/non-performance-reason-types")
@Tag(name = "danh-muc")
public class NonPerformanceReasonController {
    private final NonPerformanceReasonService service;

    public NonPerformanceReasonController(NonPerformanceReasonService service) {
        this.service = service;
    }

    @GetMapping
    public List<NonPerformanceReasonTypeResponse> list() {
        return service.list().stream().map(NonPerformanceReasonTypeResponse::of).toList();
    }

    @PostMapping
    @Operation(
            parameters =
                    @Parameter(
                            name = "Idempotency-Key",
                            in = io.swagger.v3.oas.annotations.enums.ParameterIn.HEADER,
                            required = true,
                            schema = @Schema(type = "string")))
    @ApiResponse(responseCode = "201", description = "Đã tạo", content = @Content)
    @ApiResponse(
            responseCode = "409",
            description = "Tên trùng",
            content =
                    @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemBody.class)))
    public ResponseEntity<Void> create(
            @RequestHeader("Idempotency-Key") String idempotencyKey, @RequestBody NonPerformanceReasonWrite body) {
        var row = service.create(body.name());
        return ResponseEntity.status(201).eTag(VersionEtag.of(row.getVersion())).build();
    }

    @PutMapping("/{id}")
    @Operation(
            parameters =
                    @Parameter(
                            name = "Idempotency-Key",
                            in = io.swagger.v3.oas.annotations.enums.ParameterIn.HEADER,
                            required = true,
                            schema = @Schema(type = "string")))
    @ApiResponse(responseCode = "200", description = "Đã sửa", content = @Content)
    @ApiResponse(
            responseCode = "409",
            description = "Tên trùng hoặc loại cố định",
            content =
                    @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemBody.class)))
    @ApiResponse(
            responseCode = "412",
            description = "Phiên bản đã đổi",
            content =
                    @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemBody.class)))
    public ResponseEntity<Void> rename(
            @PathVariable long id,
            @RequestHeader("If-Match") String ifMatch,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestBody NonPerformanceReasonWrite body) {
        var row = service.rename(id, ifMatch, body.name());
        return ResponseEntity.ok().eTag(VersionEtag.of(row.getVersion())).build();
    }

    @PostMapping("/{id}/deactivate")
    @Operation(
            parameters =
                    @Parameter(
                            name = "Idempotency-Key",
                            in = io.swagger.v3.oas.annotations.enums.ParameterIn.HEADER,
                            required = true,
                            schema = @Schema(type = "string")))
    @ApiResponse(responseCode = "200", description = "Đã ngừng", content = @Content)
    @ApiResponse(
            responseCode = "409",
            description = "Loại cố định",
            content =
                    @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemBody.class)))
    public ResponseEntity<Void> deactivate(
            @PathVariable long id,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestBody CareCatalogReason body) {
        service.deactivate(id, body.reason());
        return ResponseEntity.ok().build();
    }
}
