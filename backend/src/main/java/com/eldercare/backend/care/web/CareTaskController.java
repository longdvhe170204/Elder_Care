package com.eldercare.backend.care.web;

import com.eldercare.backend.administration.account.api.*;
import com.eldercare.backend.administration.account.api.dto.*;
import com.eldercare.backend.administration.account.api.spi.BedAllocationApi;
import com.eldercare.backend.administration.shared.api.StatusHistoryApi;
import com.eldercare.backend.care.api.dto.TaskStatus;
import com.eldercare.backend.care.internal.task.domain.*;
import com.eldercare.backend.care.internal.task.service.*;
import com.eldercare.backend.care.web.request.*;
import com.eldercare.backend.care.web.response.*;
import com.eldercare.backend.care.web.response.CareProblemBody;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.error.*;
import com.eldercare.backend.resident.api.ResidentApi;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import java.util.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** T025: bảy operation đã được chủ dự án duyệt, không có gợi ý/chép công việc. */
@RestController
@RequestMapping("/api/v1")
public class CareTaskController {
    private final CareTaskService commands;
    private final CareTaskQueries queries;
    private final CurrentUser user;
    private final PermissionApi permissions;
    private final FieldVisibilityApi visibility;
    private final ResidentApi residents;
    private final AccountApi accounts;
    private final BedAllocationApi beds;
    private final StatusHistoryApi history;
    private final com.eldercare.backend.care.internal.result.service.CareResultQueries results;
    private final com.eldercare.backend.care.internal.result.service.CareResultService recording;
    private final com.eldercare.backend.care.internal.checklist.service.CareChecklistService checklist;
    private final com.eldercare.backend.administration.correction.api.CorrectionApi corrections;
    private final SharedCareTasks shared;
    private final com.eldercare.backend.care.internal.timetable.service.CareTimetable timetable;

    public CareTaskController(
            CareTaskService commands,
            CareTaskQueries queries,
            CurrentUser user,
            PermissionApi permissions,
            FieldVisibilityApi visibility,
            ResidentApi residents,
            AccountApi accounts,
            BedAllocationApi beds,
            StatusHistoryApi history,
            com.eldercare.backend.care.internal.result.service.CareResultQueries results,
            com.eldercare.backend.care.internal.result.service.CareResultService recording,
            com.eldercare.backend.care.internal.checklist.service.CareChecklistService checklist,
            com.eldercare.backend.administration.correction.api.CorrectionApi corrections,
            SharedCareTasks shared,
            com.eldercare.backend.care.internal.timetable.service.CareTimetable timetable) {
        this.commands = commands;
        this.queries = queries;
        this.user = user;
        this.permissions = permissions;
        this.visibility = visibility;
        this.residents = residents;
        this.accounts = accounts;
        this.beds = beds;
        this.history = history;
        this.results = results;
        this.recording = recording;
        this.checklist = checklist;
        this.corrections = corrections;
        this.shared = shared;
        this.timetable = timetable;
    }

    @GetMapping("/shifts/{shiftId}/source-data")
    public CareShiftSourceResponse sources(@PathVariable long shiftId) {
        return CareShiftSourceResponse.of(queries.sources(commands.shiftTarget(shiftId), shiftId));
    }

    @GetMapping("/shifts/{shiftId}/care-tasks")
    public List<CareTaskResponse> list(@PathVariable long shiftId) {
        return queries.inShift(commands.shiftTarget(shiftId), shiftId).stream()
                .map(this::response)
                .toList();
    }

    @PostMapping("/shifts/{shiftId}/care-tasks/plan")
    @ApiResponse(
            responseCode = "403",
            content =
                    @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = CareProblemBody.class)))
    @ApiResponse(
            responseCode = "409",
            content =
                    @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = CareProblemBody.RowErrors.class)))
    @ApiResponse(responseCode = "201", description = "Đã lập toàn bộ lô")
    public ResponseEntity<List<CareTaskResponse>> plan(
            @PathVariable long shiftId,
            @RequestHeader("Idempotency-Key") String key,
            @RequestBody List<CareTaskWrite> body) {
        if (body == null || body.isEmpty() || body.stream().anyMatch(Objects::isNull))
            throw new InvalidValueException("Cần ít nhất một dòng công việc hợp lệ");
        return ResponseEntity.status(201)
                .body(
                        commands
                                .plan(
                                        commands.shiftTarget(shiftId),
                                        shiftId,
                                        body.stream().map(CareTaskWrite::draft).toList())
                                .stream()
                                .map(this::response)
                                .toList());
    }

    @PostMapping("/care-tasks/ad-hoc")
    @ApiResponse(
            responseCode = "409",
            content =
                    @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = CareProblemBody.class)))
    @ApiResponse(responseCode = "201", description = "Đã tạo công việc phát sinh")
    public ResponseEntity<CareTaskResponse> adHoc(
            @RequestHeader("Idempotency-Key") String key, @RequestBody CareTaskWrite body) {
        var row = body.draft();
        Long floor = row.residentId() == null
                ? row.locationId()
                : beds.openAt(row.residentId(), row.plannedAt())
                        .map(b -> b.floorAreaId())
                        .orElse(null);
        if (floor == null) throw new InvalidValueException("Chưa xác định được tầng/khu vực của công việc");
        return ResponseEntity.status(201)
                .body(response(commands.adHoc(new Target("M04.TASK", "0", row.residentId(), floor), row)));
    }

    @PostMapping("/care-tasks/{id}/rework")
    @ApiResponse(
            responseCode = "409",
            content =
                    @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = CareProblemBody.class)))
    @ApiResponse(responseCode = "201", description = "Đã lập công việc làm lại")
    public ResponseEntity<Void> rework(
            @PathVariable long id,
            @RequestHeader("Idempotency-Key") String key,
            @RequestBody CareTaskReworkWrite body) {
        if (body.qualityCheckResultId() == null || body.plannedAt() == null)
            throw new InvalidValueException("Thiếu kết quả kiểm tra hoặc thời điểm làm lại");
        commands.rework(
                commands.target(id),
                id,
                body.qualityCheckResultId(),
                body.plannedAt(),
                body.performerId(),
                Boolean.TRUE.equals(body.shared()));
        return ResponseEntity.status(201).build();
    }

    @GetMapping("/care-tasks/{id}")
    public CareTaskDetailResponse get(@PathVariable long id) {
        var task = queries.get(commands.target(id), id);
        boolean health = health(task);
        var timeline = history.list(CareTaskService.HISTORY, id);
        var closure = queries.closure(commands.target(id), id);
        var last = timeline.isEmpty() ? null : timeline.getLast();
        var rows = timeline.stream()
                .map(h -> new CareTaskDetailResponse.Handling(
                        h.command(),
                        h.actorId() == null ? "Hệ thống" : actor(h.actorId()),
                        h.basisType(),
                        h.startsAt(),
                        health ? h.reason() : null))
                .toList();
        return new CareTaskDetailResponse(
                response(task),
                results.original(CareTaskService.target(task), id)
                        .map(r -> CareResultResponse.of(
                                r,
                                actor(r.recordedBy()),
                                health,
                                corrections
                                        .current("M04.CARE_RESULT", List.of(id))
                                        .get(id)))
                        .orElse(null),
                health ? closure.reason() : null,
                closure.at() == null || last == null
                        ? null
                        : last.actorId() == null ? "Hệ thống" : actor(last.actorId()),
                closure.rule(),
                rows,
                health ? List.of() : List.of("content", "reason"));
    }

    @PostMapping("/care-tasks/{id}/cancel")
    @ApiResponse(responseCode = "200", description = "OK")
    @ApiResponse(
            responseCode = "403",
            content =
                    @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = CareProblemBody.class)))
    @ApiResponse(
            responseCode = "409",
            content =
                    @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = CareProblemBody.class)))
    public void cancel(
            @PathVariable long id, @RequestHeader("Idempotency-Key") String key, @RequestBody CareCatalogReason body) {
        commands.cancel(commands.target(id), id, body.reason());
    }

    @PostMapping("/care-tasks/{id}/reassign")
    @ApiResponse(responseCode = "200", description = "Đã phân lại")
    @ApiResponse(
            responseCode = "403",
            content =
                    @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = CareProblemBody.class)))
    @ApiResponse(
            responseCode = "409",
            content =
                    @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = CareProblemBody.class)))
    public void reassign(
            @PathVariable long id,
            @RequestHeader("Idempotency-Key") String key,
            @RequestBody CareTaskReassignWrite body) {
        if (body.recipientId() == null) throw new InvalidValueException("Thiếu người nhận");
        shared.reassign(commands.target(id), id, body.recipientId(), body.reason());
    }

    @GetMapping("/residents/{residentId}/timetable")
    @ApiResponse(responseCode = "200", description = "OK")
    public List<TimetableItemResponse> timetable(
            @PathVariable long residentId, @RequestParam java.time.LocalDate date) {
        return timetable
                .view(
                        com.eldercare.backend.care.internal.timetable.service.CareTimetable.target(residentId),
                        residentId,
                        date)
                .stream()
                .map(TimetableItemResponse::of)
                .toList();
    }

    private String actor(long id) {
        return accounts.fullName(id).orElse(Long.toString(id));
    }

    private boolean health(CareTask row) {
        return visibility.canView(user, InfoGroup.HEALTH, CareTaskService.target(row)) == Visibility.FULL;
    }

    private CareTaskResponse response(CareTask row) {
        var available = new LinkedHashMap<String, PermissionKey>();
        if (recording.canRecord(row)) {
            available.put("record", new PermissionKey(Function.TASK_CHECKLIST, Operation.CONFIRM));
            available.put("notPerform", new PermissionKey(Function.TASK_CHECKLIST, Operation.CONFIRM));
        }
        if (checklist.canClaim(row))
            available.put("claim", new PermissionKey(Function.TASK_CHECKLIST, Operation.CONFIRM));
        if (row.status() == TaskStatus.NOT_YET_DUE && user.roles().contains(Role.FLOOR_MANAGER))
            available.put("cancel", new PermissionKey(Function.SHIFT_TASK_PLANNING, Operation.UPDATE));
        CareTaskResponse.ResidentCard card = null;
        if (row.residentId() != null)
            card = new CareTaskResponse.ResidentCard(
                    residents.identity(row.residentId()).fullName(), null, null, null);
        String performer = row.performerId() == null
                ? null
                : accounts.byStaff(row.performerId())
                        .map(a -> actor(a.accountId()))
                        .orElse(Long.toString(row.performerId()));
        return CareTaskResponse.of(
                row,
                card,
                performer,
                health(row),
                permissions.allowedCommands(user, CareTaskService.target(row), available));
    }
}
