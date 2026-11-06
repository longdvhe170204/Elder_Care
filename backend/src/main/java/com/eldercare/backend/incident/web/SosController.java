package com.eldercare.backend.incident.web;

import com.eldercare.backend.administration.account.api.BoundDeviceApi;
import com.eldercare.backend.administration.account.api.dto.ResidentDevice;
import com.eldercare.backend.common.error.InvalidValueException;
import com.eldercare.backend.incident.internal.alert.service.AlertLocationService;
import com.eldercare.backend.incident.internal.alert.service.SosService;
import com.eldercare.backend.incident.web.request.LocationReportRequest;
import com.eldercare.backend.incident.web.request.SosPlaceRequest;
import com.eldercare.backend.incident.web.response.M05ProblemBody;
import com.eldercare.backend.incident.web.response.SosProblemBody;
import com.eldercare.backend.incident.web.response.SosStatusResponse;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Nút SOS của người cao tuổi (US12; FR-047d → FR-047g; research S-23). Không cần phiên: 002 cho {@code /sos/**} qua bộ
 * lọc phiên, người cao tuổi xác định bằng header {@code X-Device-Token} qua {@link BoundDeviceApi}; lớp này nằm trong
 * ngoại lệ {@code @RequiresPermission} của 002 ({@code RequiresPermissionArchTest.SOS_CONTROLLER}).
 */
@RestController
@RequestMapping("/api/v1/sos")
public class SosController {
    static final String DEVICE_TOKEN = "X-Device-Token";
    private static final String PROBLEM = "application/problem+json";

    private final BoundDeviceApi devices;
    private final SosService sos;
    private final AlertLocationService locations;

    public SosController(BoundDeviceApi devices, SosService sos, AlertLocationService locations) {
        this.devices = devices;
        this.sos = sos;
        this.locations = locations;
    }

    @PostMapping
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Đã gửi"),
        @ApiResponse(
                responseCode = "401",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = SosProblemBody.class))),
        @ApiResponse(
                responseCode = "409",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = SosProblemBody.class)))
    })
    public ResponseEntity<SosStatusResponse> press(
            @Parameter(hidden = true) @RequestHeader(value = DEVICE_TOKEN, required = false) String token,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        ResidentDevice device = device(token);
        var status = sos.press(device.residentId(), device.accountId(), device.deviceId(), idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED).body(SosStatusResponse.of(status));
    }

    @GetMapping("/current")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "204", description = "Không có cảnh báo SOS đang mở", content = @Content)
    })
    public ResponseEntity<SosStatusResponse> current(
            @Parameter(hidden = true) @RequestHeader(value = DEVICE_TOKEN, required = false) String token) {
        return sos.current(device(token).residentId())
                .map(status -> ResponseEntity.ok(SosStatusResponse.of(status)))
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PostMapping("/{id}/place")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Đã ghi"),
        @ApiResponse(
                responseCode = "409",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = M05ProblemBody.class)))
    })
    public ResponseEntity<Void> place(
            @Parameter(hidden = true) @RequestHeader(value = DEVICE_TOKEN, required = false) String token,
            @PathVariable long id,
            @RequestBody SosPlaceRequest request) {
        ResidentDevice device = device(token);
        if (request == null || request.place() == null) throw new InvalidValueException("Chưa chọn nơi đang ở");
        sos.place(device.residentId(), id, request.place());
        return ResponseEntity.noContent().build();
    }

    /** Vị trí GPS cho lần bấm (FR-047h, BR-M05-28); đã tới nơi hoặc cảnh báo kết thúc thì 409 để app dừng gửi. */
    @PostMapping("/{id}/location")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Đã lưu"),
        @ApiResponse(
                responseCode = "409",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = M05ProblemBody.class)))
    })
    public ResponseEntity<Void> location(
            @Parameter(hidden = true) @RequestHeader(value = DEVICE_TOKEN, required = false) String token,
            @PathVariable long id,
            @RequestBody LocationReportRequest request) {
        ResidentDevice device = device(token);
        locations.reportSos(device.residentId(), id, request == null ? null : request.toReport());
        return ResponseEntity.noContent().build();
    }

    /** Token sai, thiết bị đã gỡ hoặc tài khoản Đã khóa: 401 kèm số liên hệ (FR-047d). */
    private ResidentDevice device(String token) {
        return devices.identify(token == null ? "" : token)
                .orElseThrow(() ->
                        sos.refused(HttpStatus.UNAUTHORIZED, "INVALID_DEVICE", "Thiết bị chưa được gắn hoặc đã bị gỡ"));
    }
}
