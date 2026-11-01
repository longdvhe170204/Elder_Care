package com.eldercare.backend.care.web;

import com.eldercare.backend.administration.account.api.*;
import com.eldercare.backend.administration.account.api.dto.*;
import com.eldercare.backend.administration.account.api.spi.BedAllocationApi;
import com.eldercare.backend.care.internal.result.service.*;
import com.eldercare.backend.care.internal.task.service.CareTaskService;
import com.eldercare.backend.care.web.request.*;
import com.eldercare.backend.care.web.response.*;
import com.eldercare.backend.common.error.InvalidValueException;
import com.eldercare.backend.common.time.BusinessTime;
import java.time.Instant;
import java.util.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** T039: lệnh qua service, response dùng cùng projection với chi tiết task. */
@RestController
@RequestMapping("/api/v1")
public class CareResultController {
    private final CareResultService commands;
    private final CareTaskService tasks;
    private final CareTaskController taskDetails;
    private final CareResultQueries queries;
    private final BedAllocationApi beds;
    private final AccountApi accounts;
    private final CurrentUser user;
    private final FieldVisibilityApi visibility;
    private final AdHocCareRecordingService adHoc;
    private final BusinessTime time;

    public CareResultController(
            CareResultService commands,
            CareTaskService tasks,
            CareTaskController taskDetails,
            CareResultQueries queries,
            BedAllocationApi beds,
            AccountApi accounts,
            CurrentUser user,
            FieldVisibilityApi visibility,
            AdHocCareRecordingService adHoc,
            BusinessTime time) {
        this.commands = commands;
        this.tasks = tasks;
        this.taskDetails = taskDetails;
        this.queries = queries;
        this.beds = beds;
        this.accounts = accounts;
        this.user = user;
        this.visibility = visibility;
        this.adHoc = adHoc;
        this.time = time;
    }

    @PostMapping("/care-tasks/{id}/record")
    public ResponseEntity<CareTaskDetailResponse> record(
            @PathVariable long id, @RequestHeader("Idempotency-Key") String key, @RequestBody CareResultWrite body) {
        if (body == null) throw new InvalidValueException("Cần nội dung ghi nhận");
        boolean awaiting = commands.record(tasks.target(id), id, body.input());
        return awaiting ? ResponseEntity.accepted().build() : ResponseEntity.ok(taskDetails.get(id));
    }

    @PostMapping("/ad-hoc-care-recordings")
    public ResponseEntity<?> adHoc(
            @RequestHeader("Idempotency-Key") String key, @RequestBody AdHocCareRecordingWrite body) {
        if (body == null || body.residentId() == null || body.taskTypeId() == null)
            throw new InvalidValueException("Cần người cao tuổi và loại công việc");
        var result = adHoc.record(
                adHoc.target(body.residentId(), body.performedAt()),
                body.residentId(),
                body.taskTypeId(),
                body.input(),
                Boolean.TRUE.equals(body.sameTypeTasksReviewed()),
                body.originalTaskId());
        if (result.taskId() == null)
            return ResponseEntity.ok(Map.of(
                    "sameTypeTasks",
                    result.sameTypeTasks().stream()
                            .map(task -> taskDetails.get(task.id()).task())
                            .toList()));
        return ResponseEntity.status(201).body(taskDetails.get(result.taskId()));
    }

    @PostMapping("/care-tasks/{id}/not-perform")
    public void notPerform(
            @PathVariable long id,
            @RequestHeader("Idempotency-Key") String key,
            @RequestBody CareNotPerformedWrite body) {
        if (body == null) throw new InvalidValueException("Cần loại lý do Không thực hiện");
        commands.notPerformed(tasks.target(id), id, body.reasonTypeId(), body.note());
    }

    @GetMapping("/residents/{residentId}/care-results")
    public List<CareResultResponse> history(
            @PathVariable long residentId,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(required = false) Set<String> types) {
        if (from != null && to != null && from.isAfter(to)) throw new InvalidValueException("Khoảng thời gian bị đảo");
        var target = new Target(
                "M01.RESIDENT",
                Long.toString(residentId),
                residentId,
                beds.openAt(residentId, time.now()).map(b -> b.floorAreaId()).orElse(null));
        boolean health = visibility.canView(user, InfoGroup.HEALTH, target) == Visibility.FULL;
        return queries.history(target, residentId, from, to, types == null ? Set.of() : types).stream()
                .map(r -> CareResultResponse.of(
                        r, accounts.fullName(r.recordedBy()).orElse(Long.toString(r.recordedBy())), health))
                .toList();
    }
}
