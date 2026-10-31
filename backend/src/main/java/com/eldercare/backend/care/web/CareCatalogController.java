package com.eldercare.backend.care.web;

import com.eldercare.backend.care.internal.catalog.service.CareCatalogService;
import com.eldercare.backend.care.web.request.*;
import com.eldercare.backend.care.web.response.*;
import com.eldercare.backend.common.web.VersionEtag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** T007: đúng tám operation danh mục,không thêm xóa/đọc chi tiết/kích hoạt lại. */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "danh-muc")
public class CareCatalogController {
    private final CareCatalogService service;

    public CareCatalogController(CareCatalogService service) {
        this.service = service;
    }

    @GetMapping("/task-types")
    public List<TaskTypeResponse> taskTypes() {
        return service.taskTypes().stream().map(TaskTypeResponse::of).toList();
    }

    @GetMapping("/result-values")
    public List<ResultValueResponse> resultValues() {
        return service.resultValues().stream().map(ResultValueResponse::of).toList();
    }

    @PostMapping("/task-types")
    @ApiResponse(responseCode = "201", description = "Đã tạo")
    @Operation(
            parameters =
                    @Parameter(
                            name = "Idempotency-Key",
                            in = ParameterIn.HEADER,
                            required = true,
                            schema = @Schema(type = "string")))
    public ResponseEntity<Void> createTaskType(
            @RequestHeader("Idempotency-Key") String key, @RequestBody TaskTypeWrite body) {
        var row = service.createTaskType(body.name(), body.attributes());
        return ResponseEntity.status(201).eTag(VersionEtag.of(row.version())).build();
    }

    @PostMapping("/result-values")
    @ApiResponse(responseCode = "201", description = "Đã tạo")
    @Operation(
            parameters =
                    @Parameter(
                            name = "Idempotency-Key",
                            in = ParameterIn.HEADER,
                            required = true,
                            schema = @Schema(type = "string")))
    public ResponseEntity<Void> createResultValue(
            @RequestHeader("Idempotency-Key") String key, @RequestBody ResultValueWrite body) {
        var row = service.createResultValue(body.name(), body.attributes());
        return ResponseEntity.status(201).eTag(VersionEtag.of(row.version())).build();
    }

    @PutMapping("/task-types/{id}")
    @Operation(
            parameters =
                    @Parameter(
                            name = "Idempotency-Key",
                            in = ParameterIn.HEADER,
                            required = true,
                            schema = @Schema(type = "string")))
    public ResponseEntity<Void> updateTaskType(
            @PathVariable long id,
            @RequestHeader("If-Match") String etag,
            @RequestHeader("Idempotency-Key") String key,
            @RequestBody TaskTypeWrite body) {
        var row = service.updateTaskType(id, etag, body.name(), body.attributes());
        return ResponseEntity.ok().eTag(VersionEtag.of(row.version())).build();
    }

    @PutMapping("/result-values/{id}")
    @Operation(
            parameters =
                    @Parameter(
                            name = "Idempotency-Key",
                            in = ParameterIn.HEADER,
                            required = true,
                            schema = @Schema(type = "string")))
    public ResponseEntity<Void> updateResultValue(
            @PathVariable long id,
            @RequestHeader("If-Match") String etag,
            @RequestHeader("Idempotency-Key") String key,
            @RequestBody ResultValueWrite body) {
        var row = service.updateResultValue(id, etag, body.name(), body.attributes());
        return ResponseEntity.ok().eTag(VersionEtag.of(row.version())).build();
    }

    @PostMapping("/task-types/{id}/deactivate")
    @Operation(
            parameters =
                    @Parameter(
                            name = "Idempotency-Key",
                            in = ParameterIn.HEADER,
                            required = true,
                            schema = @Schema(type = "string")))
    public ResponseEntity<Void> deactivateTaskType(
            @PathVariable long id, @RequestHeader("Idempotency-Key") String key, @RequestBody CareCatalogReason body) {
        service.deactivateTaskType(id, body.reason());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/result-values/{id}/deactivate")
    @Operation(
            parameters =
                    @Parameter(
                            name = "Idempotency-Key",
                            in = ParameterIn.HEADER,
                            required = true,
                            schema = @Schema(type = "string")))
    public ResponseEntity<Void> deactivateResultValue(
            @PathVariable long id, @RequestHeader("Idempotency-Key") String key, @RequestBody CareCatalogReason body) {
        service.deactivateResultValue(id, body.reason());
        return ResponseEntity.ok().build();
    }
}
