package com.eldercare.backend.staffing.shift.web;

import com.eldercare.backend.administration.account.api.*;
import com.eldercare.backend.administration.account.api.dto.*;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.common.web.VersionEtag;
import com.eldercare.backend.staffing.duty.internal.service.ShiftQueries;
import com.eldercare.backend.staffing.shift.internal.domain.*;
import com.eldercare.backend.staffing.shift.internal.service.ShiftScheduleService;
import com.eldercare.backend.staffing.shift.web.request.ShiftRequests;
import com.eldercare.backend.staffing.shift.web.response.ShiftResponses;
import com.eldercare.backend.staffing.web.response.M09ProblemBody;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.*;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** T016: chỉ các endpoint thuộc lập/công bố lịch; lệnh sau công bố thuộc T041. */
@RestController
@RequestMapping("/api/v1")
class ShiftController {
    private final ShiftScheduleService schedules;
    private final ShiftQueries query;
    private final BusinessTime time;
    private final ObjectProvider<CurrentUser> users;
    private final PermissionApi permissions;
    private final com.eldercare.backend.staffing.handover.internal.service.HandoverService handovers;
    private final com.eldercare.backend.staffing.ratio.internal.service.StaffingRatioService ratios;
    private final com.eldercare.backend.staffing.shift.internal.service.PostPublicationService postPublication;

    ShiftController(
            ShiftScheduleService schedules,
            ShiftQueries query,
            BusinessTime time,
            ObjectProvider<CurrentUser> users,
            PermissionApi permissions,
            com.eldercare.backend.staffing.handover.internal.service.HandoverService handovers,
            com.eldercare.backend.staffing.ratio.internal.service.StaffingRatioService ratios,
            com.eldercare.backend.staffing.shift.internal.service.PostPublicationService postPublication) {
        this.schedules = schedules;
        this.query = query;
        this.time = time;
        this.users = users;
        this.permissions = permissions;
        this.handovers = handovers;
        this.ratios = ratios;
        this.postPublication = postPublication;
    }

    @GetMapping("/shift-templates")
    List<ShiftResponses.Template> templates() {
        return schedules.templates().stream().map(ShiftResponses.Template::of).toList();
    }

    @PostMapping("/shift-templates")
    @Operation(
            requestBody =
                    @io.swagger.v3.oas.annotations.parameters.RequestBody(
                            required = true,
                            content =
                                    @Content(
                                            mediaType = "application/json",
                                            schema = @Schema(implementation = ShiftResponses.Template.class))))
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Đã tạo", content = @Content),
        @ApiResponse(
                responseCode = "422",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = M09ProblemBody.class)))
    })
    ResponseEntity<Void> createTemplate(
            @RequestHeader("Idempotency-Key") UUID key, @Valid @RequestBody ShiftRequests.Template input) {
        var template = schedules.createTemplate(input.input());
        return ResponseEntity.created(URI.create("/api/v1/shift-templates/" + template.id()))
                .build();
    }

    @PutMapping("/shift-templates/{id}")
    @Operation(
            requestBody =
                    @io.swagger.v3.oas.annotations.parameters.RequestBody(
                            required = true,
                            content =
                                    @Content(
                                            mediaType = "application/json",
                                            schema = @Schema(implementation = ShiftResponses.Template.class))))
    ResponseEntity<Void> updateTemplate(
            @PathVariable long id,
            @RequestHeader("If-Match") String match,
            @RequestHeader("Idempotency-Key") UUID key,
            @Valid @RequestBody ShiftRequests.Template input) {
        var template = schedules.updateTemplate(id, match, input.input());
        return ResponseEntity.ok().eTag(VersionEtag.of(template.version())).build();
    }

    @GetMapping("/shift-schedules")
    List<ShiftResponses.Schedule> list(@RequestParam String month, @RequestParam(required = false) Long scopeId) {
        return schedules.schedules(ShiftRequests.parseMonth(month), scopeId).stream()
                .map(s -> new ShiftResponses.Schedule(
                        s.id(),
                        s.month().toString(),
                        s.scopeType(),
                        s.scopeId(),
                        s.status(),
                        schedules.scheduleShifts(s.id()).stream()
                                .map(this::body)
                                .toList(),
                        s.version()))
                .toList();
    }

    @PostMapping("/shift-schedules")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Đã lập", content = @Content),
        @ApiResponse(
                responseCode = "409",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = M09ProblemBody.class)))
    })
    ResponseEntity<Void> createSchedule(
            @RequestHeader("Idempotency-Key") UUID key, @Valid @RequestBody ShiftRequests.Schedule input) {
        var target = new Target("M09.SCHEDULE", null, null, input.scopeId());
        var schedule = schedules.createSchedule(input.monthValue(), input.scopeType(), input.scopeId(), target);
        return ResponseEntity.created(URI.create("/api/v1/shift-schedules/" + schedule.id()))
                .build();
    }

    @PostMapping("/shift-schedules/{id}/shifts")
    @ApiResponses({
        @ApiResponse(
                responseCode = "201",
                description = "Đã thêm",
                content =
                        @Content(
                                mediaType = "application/json",
                                schema = @Schema(implementation = ShiftResponses.Result.class))),
        @ApiResponse(
                responseCode = "409",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = M09ProblemBody.class))),
        @ApiResponse(
                responseCode = "412",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = M09ProblemBody.class)))
    })
    ResponseEntity<ShiftResponses.Result> addShift(
            @PathVariable long id,
            @RequestHeader("If-Match") String match,
            @RequestHeader("Idempotency-Key") UUID key,
            @Valid @RequestBody ShiftRequests.Shift input) {
        var result = schedules.addShift(id, match, input.input(), schedules.targetOfSchedule(id));
        return ResponseEntity.created(URI.create("/api/v1/shifts/" + result.shiftId()))
                .body(new ShiftResponses.Result(result.shiftId(), ShiftResponses.warnings(result.warnings())));
    }

    @GetMapping("/shifts/{id}")
    ResponseEntity<ShiftResponses.Shift> detail(@PathVariable long id) {
        var shift = schedules.detail(id);
        return ResponseEntity.ok().eTag(VersionEtag.of(shift.version())).body(body(shift));
    }

    @GetMapping("/shifts/{id}/staffing-ratios")
    List<ShiftResponses.StaffingRatio> staffingRatios(@PathVariable long id) {
        schedules.detail(id);
        return ratios.history(id).stream().map(ShiftResponses.StaffingRatio::of).toList();
    }

    @PutMapping("/shifts/{id}")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Đã lưu"),
        @ApiResponse(
                responseCode = "409",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = M09ProblemBody.class))),
        @ApiResponse(
                responseCode = "412",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = M09ProblemBody.class)))
    })
    ShiftResponses.Result editShift(
            @PathVariable long id,
            @RequestHeader("If-Match") String match,
            @RequestHeader("Idempotency-Key") UUID key,
            @Valid @RequestBody ShiftRequests.Shift input) {
        var result = schedules.editShift(id, match, input.input(), schedules.targetOfShift(id));
        return new ShiftResponses.Result(result.shiftId(), ShiftResponses.warnings(result.warnings()));
    }

    @DeleteMapping("/shifts/{id}")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Đã xoá", content = @Content),
        @ApiResponse(
                responseCode = "409",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = M09ProblemBody.class)))
    })
    ResponseEntity<Void> deleteShift(@PathVariable long id, @RequestHeader("If-Match") String match) {
        schedules.deleteShift(id, match, schedules.targetOfShift(id));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/shift-schedules/{id}/publication-preview")
    ShiftResponses.Preview preview(@PathVariable long id) {
        var preview = schedules.preview(id);
        return new ShiftResponses.Preview(
                preview.unmetConditions().stream().map(c -> (Object) c).toList(),
                preview.invalidAssignments().stream()
                        .map(a -> new ShiftResponses.InvalidAssignment(a.assignmentId(), a.reason()))
                        .toList(),
                ShiftResponses.warnings(preview.warnings()));
    }

    @PostMapping("/shift-schedules/{id}/publish")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Đã công bố", content = @Content),
        @ApiResponse(
                responseCode = "403",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = M09ProblemBody.class))),
        @ApiResponse(
                responseCode = "409",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = M09ProblemBody.class)))
    })
    ResponseEntity<Void> publish(
            @PathVariable long id,
            @RequestHeader("Idempotency-Key") UUID key,
            @RequestBody ShiftRequests.Publish input) {
        schedules.publish(
                id,
                input.assignmentIdsToRemove() == null ? List.of() : input.assignmentIdsToRemove(),
                Boolean.TRUE.equals(input.confirmWarnings()),
                input.reason(),
                schedules.targetOfSchedule(id));
        return ResponseEntity.ok().build();
    }

    @GetMapping("/me/shifts")
    @ApiResponse(responseCode = "200", description = "OK", content = @Content)
    List<ShiftResponses.Shift> mine(
            @RequestParam(required = false) LocalDate from, @RequestParam(required = false) LocalDate to) {
        LocalDate begin = from == null ? time.today() : from;
        LocalDate end = to == null ? begin.plusMonths(1) : to;
        return schedules.ownShifts(begin, end).stream().map(this::body).toList();
    }

    @GetMapping("/on-duty-doctors")
    @ApiResponse(responseCode = "200", description = "OK", content = @Content)
    List<Long> doctors(@RequestParam(required = false) Instant at) {
        return query.doctors(at == null ? time.now() : at);
    }

    private ShiftResponses.Shift body(WorkShift shift) {
        List<String> commands = List.of();
        if ("DRAFT".equals(shift.status()))
            commands = permissions
                    .allowedCommands(
                            users.getObject(),
                            schedules.targetOfShift(shift.id()),
                            Map.of(
                                    "edit",
                                    new PermissionKey(
                                            Function.SHIFT_SCHEDULE,
                                            com.eldercare.backend.administration.account.api.dto.Operation.UPDATE),
                                    "delete",
                                    new PermissionKey(
                                            Function.SHIFT_SCHEDULE,
                                            com.eldercare.backend.administration.account.api.dto.Operation.UPDATE)))
                    .stream()
                    .sorted()
                    .toList();
        var assignmentCommands = new ArrayList<>(commands);
        assignmentCommands.addAll(postPublication.allowedCommands(shift));
        assignmentCommands.addAll(schedules.assignmentCommands(shift));
        if (handovers.canCreate(shift.id())) assignmentCommands.add("createHandover");
        commands = List.copyOf(assignmentCommands);
        var members = schedules.members(shift.id()).stream()
                .map(m -> new ShiftResponses.Staff(
                        m.staffId(), m.onDutyDoctor(), m.absentAt() != null, m.approvedLeaveRequestId() != null))
                .toList();
        return new ShiftResponses.Shift(
                shift.id(),
                shift.shiftTemplateId(),
                shift.startsAt(),
                shift.endsAt(),
                shift.status(),
                shift.leadStaffId(),
                shift.missingLead(),
                members,
                ratios.latest(shift.id()).map(ShiftResponses.StaffingRatio::of).orElse(null),
                commands,
                shift.version());
    }
}
