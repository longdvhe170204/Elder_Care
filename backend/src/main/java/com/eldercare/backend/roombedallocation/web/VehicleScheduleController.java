package com.eldercare.backend.roombedallocation.web;

import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.roombedallocation.internal.vehicle.service.VehicleScheduleService;
import com.eldercare.backend.roombedallocation.web.request.VehicleScheduleCommand;
import com.eldercare.backend.roombedallocation.web.request.VehicleScheduleWrite;
import com.eldercare.backend.roombedallocation.web.response.AssetProblem;
import com.eldercare.backend.roombedallocation.web.response.VehicleScheduleResponse;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** 019 T022 (api-019 tag vehicle-schedules; FR-017, FR-018): đặt lịch xe, lệnh trên lịch, lịch theo xe và khoảng. */
@RestController
@RequestMapping("/api/v1")
public class VehicleScheduleController {
    private final VehicleScheduleService schedules;

    public VehicleScheduleController(VehicleScheduleService schedules) {
        this.schedules = schedules;
    }

    @GetMapping("/vehicle-schedules")
    @ApiResponse(responseCode = "200", description = "OK")
    public List<VehicleScheduleResponse> list(
            @RequestParam(required = false) Long vehicleId,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to) {
        return schedules.list(vehicleId, from, to).stream()
                .map(VehicleScheduleResponse::of)
                .toList();
    }

    @PostMapping("/vehicle-schedules")
    @ApiResponse(responseCode = "201", description = "OK")
    @ApiResponse(
            responseCode = "409",
            content =
                    @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = AssetProblem.class)))
    public ResponseEntity<Void> book(
            @RequestHeader("Idempotency-Key") String key, @RequestBody VehicleScheduleWrite body) {
        long id = schedules.book(body.input());
        return ResponseEntity.created(URI.create("/api/v1/vehicle-schedules/" + id))
                .build();
    }

    @PostMapping("/vehicle-schedules/{id}/{command}")
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
                                                "reschedule",
                                                "cancel",
                                                "record-departure",
                                                "record-return"
                                            }))
                    String command,
            @RequestHeader("Idempotency-Key") String key,
            @RequestBody(required = false) VehicleScheduleCommand body) {
        var input = (body == null ? new VehicleScheduleCommand(null, null, null, null) : body).input();
        switch (command) {
            case "reschedule" -> schedules.reschedule(id, input);
            case "cancel" -> schedules.cancel(id, input);
            case "record-departure" -> schedules.recordDeparture(id, input);
            case "record-return" -> schedules.recordReturn(id, input);
            default -> throw new NotFoundException();
        }
    }
}
