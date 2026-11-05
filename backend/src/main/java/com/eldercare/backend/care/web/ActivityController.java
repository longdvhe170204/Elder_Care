package com.eldercare.backend.care.web;

import com.eldercare.backend.care.internal.activity.domain.TemplateVersion;
import com.eldercare.backend.care.internal.activity.service.ActivityService;
import com.eldercare.backend.care.web.request.ActivityCommandWrite;
import com.eldercare.backend.care.web.request.ActivityWrite;
import com.eldercare.backend.care.web.response.ActivityResponse;
import com.eldercare.backend.care.web.response.CareProblemBody;
import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.error.InvalidValueException;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import java.net.URI;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** 014 T004: hoạt động (api-014 tag activities); loại hoạt động chờ quyền danh mục (team-plan mục 8). */
@RestController
@RequestMapping("/api/v1")
public class ActivityController {
    private final ActivityService activities;

    public ActivityController(ActivityService activities) {
        this.activities = activities;
    }

    @GetMapping("/activities")
    @ApiResponse(responseCode = "200", description = "OK")
    public List<ActivityResponse> list(@RequestParam(required = false) Long floorAreaId) {
        return activities.list(ActivityService.target(floorAreaId), floorAreaId).stream()
                .map(ActivityResponse::of)
                .toList();
    }

    @PostMapping("/activities")
    @ApiResponse(responseCode = "201", description = "OK")
    @ApiResponse(
            responseCode = "422",
            content =
                    @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = CareProblemBody.class)))
    public ResponseEntity<Void> declare(@RequestHeader("Idempotency-Key") String key, @RequestBody ActivityWrite body) {
        // Kiểm hình dạng trước lớp quyền: thiếu tầng thì Target không có tầng và lớp phạm vi trả 404.
        if ("FLOOR".equals(body.scope()) && body.floorAreaId() == null)
            throw new InvalidValueException("Hoạt động theo tầng cần tầng/khu vực");
        var row =
                activities.declare(ActivityService.target(body.floorAreaId()), body.toActivity(), body.groupActivity());
        return ResponseEntity.created(URI.create("/api/v1/activities/" + row.id()))
                .build();
    }

    @PutMapping("/activities/{id}")
    @ApiResponse(responseCode = "200", description = "OK")
    @ApiResponse(
            responseCode = "409",
            content =
                    @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = CareProblemBody.class)))
    public void update(
            @PathVariable long id,
            @RequestHeader("If-Match") String ifMatch,
            @RequestHeader("Idempotency-Key") String key,
            @RequestBody ActivityWrite body) {
        activities.update(activities.target(id), id, ifMatch, body.toActivity(), body.groupActivity());
    }

    @PostMapping("/activities/{id}/{command}")
    @ApiResponse(responseCode = "200", description = "Kết quả lệnh")
    @ApiResponse(
            responseCode = "409",
            content =
                    @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = CareProblemBody.class)))
    public void command(
            @PathVariable long id,
            @PathVariable
                    @io.swagger.v3.oas.annotations.Parameter(
                            schema =
                                    @Schema(
                                            allowableValues = {
                                                "deactivate",
                                                "change-template",
                                                "create-sessions-from-template",
                                                "add-regular-participant",
                                                "remove-regular-participant"
                                            }))
                    String command,
            @RequestHeader("Idempotency-Key") String key,
            @RequestBody ActivityCommandWrite body) {
        var target = activities.target(id);
        var reason = body.reason() == null || body.reason().isBlank() ? null : new Reason(body.reason());
        switch (command) {
            case "deactivate" -> activities.deactivate(target, id, body.effectiveDate(), reason);
            case "change-template" -> {
                var t = body.template();
                activities.changeTemplate(
                        target,
                        id,
                        body.effectiveDate(),
                        t == null
                                ? null
                                : new TemplateVersion(
                                        t.daysOfWeek(),
                                        t.intervalDays(),
                                        t.startTime(),
                                        t.endTime(),
                                        t.startDate(),
                                        t.endDate(),
                                        null,
                                        0),
                        reason);
            }
            case "add-regular-participant" -> activities.addRegularParticipant(target, id, body.residentId());
            case "remove-regular-participant" ->
                activities.removeRegularParticipant(target, id, body.residentId(), reason);
            // US1 (T009) cài sau khi có bảng buổi; chưa làm thì báo rõ, không bỏ qua lệnh.
            case "create-sessions-from-template" ->
                throw new BusinessException(
                        HttpStatus.SERVICE_UNAVAILABLE, "CARE_SOURCE_UNAVAILABLE", "Tạo buổi từ mẫu thuộc US1 (T009)");
            default -> throw new InvalidValueException("Lệnh không hợp lệ: " + command);
        }
    }
}
