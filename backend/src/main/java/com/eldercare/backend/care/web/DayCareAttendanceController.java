package com.eldercare.backend.care.web;

import com.eldercare.backend.administration.account.api.*;
import com.eldercare.backend.administration.account.api.dto.*;
import com.eldercare.backend.care.internal.attendance.service.*;
import com.eldercare.backend.care.web.request.*;
import com.eldercare.backend.care.web.response.CareProblemBody;
import com.eldercare.backend.care.web.response.DayCareAttendanceResponse;
import com.eldercare.backend.care.web.response.DayCareCheckoutResponse;
import com.eldercare.backend.common.error.InvalidValueException;
import com.eldercare.backend.common.time.BusinessTime;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import java.time.*;
import java.util.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/day-care-attendances")
public class DayCareAttendanceController {
    private final AttendanceService commands;
    private final AttendanceSources sources;
    private final AttendanceQueries queries;
    private final BusinessTime time;
    private final PermissionApi permissions;
    private final CurrentUser user;

    public DayCareAttendanceController(
            AttendanceService commands,
            AttendanceSources sources,
            AttendanceQueries queries,
            BusinessTime time,
            PermissionApi permissions,
            CurrentUser user) {
        this.commands = commands;
        this.sources = sources;
        this.queries = queries;
        this.time = time;
        this.permissions = permissions;
        this.user = user;
    }

    @GetMapping
    public List<DayCareAttendanceResponse> list(@RequestParam LocalDate date) {
        return queries.list(new Target("M04.DAY_CARE_ATTENDANCE", null, null, sources.floor()), date).stream()
                .map(DayCareAttendanceResponse::of)
                .toList();
    }

    @PostMapping("/check-in")
    @ApiResponse(responseCode = "200", description = "OK")
    @ApiResponse(
            responseCode = "409",
            content =
                    @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = CareProblemBody.class)))
    public DayCareAttendanceResponse checkIn(
            @RequestHeader("Idempotency-Key") String key, @RequestBody DayCareCheckInWrite body) {
        if (body.residentId() == null || body.residentId() <= 0)
            throw new InvalidValueException("Cần chọn người cao tuổi");
        return DayCareAttendanceResponse.of(commands.checkIn(
                commands.target(body.residentId(), sources.floor()),
                body.residentId(),
                body.at() == null ? time.now() : body.at()));
    }

    @PostMapping("/{id}/check-out")
    @ApiResponse(responseCode = "200", description = "OK")
    @ApiResponse(
            responseCode = "409",
            content =
                    @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = CareProblemBody.class)))
    public DayCareCheckoutResponse checkOut(
            @PathVariable long id,
            @RequestHeader("Idempotency-Key") String key,
            @RequestBody DayCareCheckOutWrite body) {
        if (body.pickupRecordId() == null || body.pickupRecordId() <= 0)
            throw new InvalidValueException("Cần bản ghi đón hợp lệ");
        var row = commands.checkOut(
                commands.target(id), id, body.pickupRecordId(), body.at() == null ? time.now() : body.at());
        return new DayCareCheckoutResponse(DayCareAttendanceResponse.of(row.attendance()), row.cancelledTaskIds());
    }
}
