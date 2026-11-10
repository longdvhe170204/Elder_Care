package com.eldercare.backend.roombedallocation.web;

import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.common.error.InvalidValueException;
import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.roombedallocation.api.dto.AssetStatus;
import com.eldercare.backend.roombedallocation.internal.asset.service.AssetService;
import com.eldercare.backend.roombedallocation.internal.asset.service.AssetTypeService;
import com.eldercare.backend.roombedallocation.web.request.AssetCommandWrite;
import com.eldercare.backend.roombedallocation.web.request.AssetTypeWrite;
import com.eldercare.backend.roombedallocation.web.request.AssetWrite;
import com.eldercare.backend.roombedallocation.web.response.AssetProblem;
import com.eldercare.backend.roombedallocation.web.response.AssetResponse;
import com.eldercare.backend.roombedallocation.web.response.AssetTypeResponse;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 019 T018 lượt 1 (api-019 tag assets; FR-011 → FR-013): ghi tăng, sửa mô tả, lệnh theo bảng FR-013, danh mục loại.
 * Chi tiết {@code GET /assets/{id}}, thanh lý, gỡ dấu, gắn hư hỏng dọn giường ở lượt sau.
 */
@RestController
@RequestMapping("/api/v1")
public class AssetController {
    private final AssetService assets;
    private final AssetTypeService types;

    public AssetController(AssetService assets, AssetTypeService types) {
        this.assets = assets;
        this.types = types;
    }

    @GetMapping("/assets")
    @ApiResponse(responseCode = "200", description = "OK")
    public List<AssetResponse> list(
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Long typeId,
            @RequestParam(required = false) String status) {
        return assets.list(departmentId, typeId, status(status)).stream()
                .map(row -> AssetResponse.of(row, assets.locationLabel(row)))
                .toList();
    }

    @PostMapping("/assets")
    @ApiResponse(responseCode = "201", description = "OK")
    @ApiResponse(
            responseCode = "409",
            content =
                    @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = AssetProblem.class)))
    public ResponseEntity<Void> create(@RequestHeader("Idempotency-Key") String key, @RequestBody AssetWrite body) {
        long id = assets.create(Target.UNSPECIFIED, body.input());
        return ResponseEntity.created(URI.create("/api/v1/assets/" + id)).build();
    }

    @GetMapping("/asset-types")
    @ApiResponse(responseCode = "200", description = "OK")
    public List<AssetTypeResponse> types() {
        return types.list().stream().map(AssetTypeResponse::of).toList();
    }

    @PostMapping("/asset-types")
    @ApiResponse(responseCode = "200", description = "OK")
    public void saveType(@RequestHeader("Idempotency-Key") String key, @RequestBody AssetTypeWrite body) {
        types.save(Target.UNSPECIFIED, body.id(), body.name(), body.status());
    }

    @PutMapping("/assets/{id}")
    @ApiResponse(responseCode = "200", description = "OK")
    @ApiResponse(
            responseCode = "409",
            content =
                    @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = AssetProblem.class)))
    public void update(
            @PathVariable long id,
            @RequestHeader("If-Match") String ifMatch,
            @RequestHeader("Idempotency-Key") String key,
            @RequestBody AssetWrite body) {
        assets.update(assets.target(id), id, ifMatch, body.input());
    }

    @PostMapping("/assets/{id}/{command}")
    @ApiResponse(responseCode = "200", description = "OK")
    @ApiResponse(
            responseCode = "403",
            content =
                    @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = AssetProblem.class)))
    @ApiResponse(
            responseCode = "409",
            content =
                    @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = AssetProblem.class)))
    public void command(
            @PathVariable long id,
            @PathVariable
                    @Parameter(
                            schema =
                                    @Schema(
                                            allowableValues = {
                                                "put-into-use",
                                                "relocate",
                                                "report-damage",
                                                "start-maintenance",
                                                "record-maintenance-result",
                                                "remove-transfer-flag",
                                                "request-disposal",
                                                "transfer-department",
                                                "link-bed-preparation-damage"
                                            }))
                    String command,
            @RequestHeader("Idempotency-Key") String key,
            @RequestBody AssetCommandWrite body) {
        var target = assets.target(id);
        var input = body.input();
        switch (command) {
            case "put-into-use" -> assets.putIntoUse(target, id, input);
            case "relocate" -> assets.relocate(target, id, input);
            case "report-damage" -> assets.reportDamage(target, id, input);
            case "start-maintenance" -> assets.startMaintenance(target, id, input);
            case "record-maintenance-result" -> assets.recordMaintenanceResult(target, id, input);
            case "transfer-department" -> assets.transferDepartment(target, id, input);
            case "request-disposal" -> assets.requestDisposal(target, id, input);
            case "remove-transfer-flag" -> assets.removeTransferFlag(target, id, input);
            case "link-bed-preparation-damage" -> assets.linkBedPreparationDamage(target, id, input);
            default -> throw new NotFoundException();
        }
    }

    private static AssetStatus status(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return AssetStatus.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new InvalidValueException("Trạng thái không hợp lệ");
        }
    }
}
