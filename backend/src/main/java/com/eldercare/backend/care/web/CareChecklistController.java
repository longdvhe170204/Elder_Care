package com.eldercare.backend.care.web;

import com.eldercare.backend.administration.account.api.*;
import com.eldercare.backend.administration.account.api.dto.*;
import com.eldercare.backend.care.internal.checklist.domain.CareChecklist;
import com.eldercare.backend.care.internal.checklist.service.CareChecklistService;
import com.eldercare.backend.care.internal.task.service.CareTaskService;
import com.eldercare.backend.care.web.response.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.json.JsonMapper;

@RestController
@RequestMapping("/api/v1")
public class CareChecklistController {
    private final CareChecklistService checklist;
    private final CareTaskService tasks;
    private final CurrentUser user;
    private final AccountApi accounts;
    private final FieldVisibilityApi visibility;
    private final PermissionApi permissions;
    private final JsonMapper json;
    private final com.eldercare.backend.care.internal.result.service.CareResultService recording;

    public CareChecklistController(
            CareChecklistService checklist,
            CareTaskService tasks,
            CurrentUser user,
            AccountApi accounts,
            FieldVisibilityApi visibility,
            PermissionApi permissions,
            JsonMapper json,
            com.eldercare.backend.care.internal.result.service.CareResultService recording) {
        this.checklist = checklist;
        this.tasks = tasks;
        this.user = user;
        this.accounts = accounts;
        this.visibility = visibility;
        this.permissions = permissions;
        this.json = json;
        this.recording = recording;
    }

    @GetMapping("/checklist")
    public ResponseEntity<CareChecklistResponse> load(@RequestParam long shiftId) {
        var data = checklist.load(shiftId);
        var response = response(data);
        return ResponseEntity.ok().eTag(etag(response)).body(response);
    }

    @PostMapping("/care-tasks/{id}/claim")
    public void claim(@PathVariable long id, @RequestHeader("Idempotency-Key") String key) {
        checklist.claim(tasks.target(id), id);
    }

    private CareChecklistResponse response(CareChecklist data) {
        var health = new HashMap<Long, Boolean>();
        for (var task : data.tasks())
            if (task.residentId() != null)
                health.computeIfAbsent(
                        task.residentId(),
                        id -> visibility.canView(user, InfoGroup.HEALTH, CareTaskService.target(task))
                                == Visibility.FULL);
        String performer = accounts.fullName(user.accountId()).orElse(Long.toString(user.accountId()));
        var taskRows = data.tasks().stream()
                .map(task -> {
                    boolean canSee = task.residentId() == null
                            ? visibility.canView(user, InfoGroup.HEALTH, CareTaskService.target(task))
                                    == Visibility.FULL
                            : health.get(task.residentId());
                    CareTaskResponse.ResidentCard card = null;
                    if (task.residentId() != null) {
                        var identity = data.identities().get(task.residentId());
                        var location = data.locations().get(task.residentId());
                        String room = location == null ? null : location.roomName();
                        String bed = location == null ? null : location.bedCode();
                        String roomBed = room == null ? bed : bed == null ? room : room + " / " + bed;
                        card = new CareTaskResponse.ResidentCard(
                                identity.fullName(),
                                null,
                                roomBed,
                                canSee
                                        ? data.careNotes()
                                                .getOrDefault(task.residentId(), Optional.empty())
                                                .orElse(null)
                                        : null);
                    }
                    var available = new LinkedHashMap<String, PermissionKey>();
                    if (recording.canRecord(task)) {
                        available.put("record", new PermissionKey(Function.TASK_CHECKLIST, Operation.CONFIRM));
                        available.put("notPerform", new PermissionKey(Function.TASK_CHECKLIST, Operation.CONFIRM));
                    }
                    if (checklist.canClaim(task))
                        available.put("claim", new PermissionKey(Function.TASK_CHECKLIST, Operation.CONFIRM));
                    return CareTaskResponse.of(
                            task,
                            card,
                            task.performerId() == null ? null : performer,
                            canSee,
                            permissions.allowedCommands(user, CareTaskService.target(task), available));
                })
                .toList();
        var grouped = new LinkedHashMap<Long, List<CareChecklistResponse.Agreement>>();
        for (var agreement : data.agreements())
            if (health.computeIfAbsent(
                    agreement.residentId(),
                    id -> visibility.canView(
                                    user, InfoGroup.HEALTH, new Target("M01.RESIDENT", Long.toString(id), id, null))
                            == Visibility.FULL))
                grouped.computeIfAbsent(agreement.residentId(), id -> new ArrayList<>())
                        .add(CareChecklistResponse.Agreement.of(agreement));
        var risks = grouped.entrySet().stream()
                .map(e -> new CareChecklistResponse.ResidentAgreements(e.getKey(), List.copyOf(e.getValue())))
                .toList();
        Map<String, Object> handover = data.handoverTasks() == null
                ? Map.of()
                : Map.of(
                        "receiveAtStart",
                        data.handoverTasks().receiveAtStart(),
                        "prepareAtEnd",
                        data.handoverTasks().prepareAtEnd());
        return new CareChecklistResponse(
                data.shiftId(),
                data.loadedAt(),
                taskRows,
                risks,
                data.reasons().stream()
                        .map(NonPerformanceReasonTypeResponse::of)
                        .toList(),
                data.medicationDoses(),
                data.openAlerts(),
                handover);
    }

    private String etag(CareChecklistResponse response) {
        try {
            return '"'
                    + Base64.getUrlEncoder()
                            .withoutPadding()
                            .encodeToString(MessageDigest.getInstance("SHA-256")
                                    .digest(json.writeValueAsString(response).getBytes(StandardCharsets.UTF_8)))
                    + '"';
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
