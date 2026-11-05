package com.eldercare.backend.care.web;

import com.eldercare.backend.care.internal.activity.service.ActivityService;
import com.eldercare.backend.care.internal.session.service.ActivitySessionService;
import com.eldercare.backend.care.internal.session.service.RegistrationService;
import com.eldercare.backend.care.internal.session.service.SessionAttendanceService;
import com.eldercare.backend.care.web.request.ActivityReasonWrite;
import com.eldercare.backend.care.web.request.ActivityRegistrationWrite;
import com.eldercare.backend.care.web.request.ActivitySessionCommandWrite;
import com.eldercare.backend.care.web.request.ActivitySessionCreateWrite;
import com.eldercare.backend.care.web.request.SessionAttendanceWrite;
import com.eldercare.backend.care.web.response.ActivitySessionDetailResponse;
import com.eldercare.backend.care.web.response.ActivitySessionResponse;
import com.eldercare.backend.care.web.response.CareProblemBody;
import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.error.InvalidValueException;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** 014 T009: buổi hoạt động (api-014 tag activity-sessions); chuyến đi (US4), hoàn tất điểm danh (US3) cài sau. */
@RestController
@RequestMapping("/api/v1")
public class ActivitySessionController {
    private final ActivitySessionService sessions;
    private final ActivityService activities;
    private final RegistrationService registrations;
    private final SessionAttendanceService attendance;

    public ActivitySessionController(
            ActivitySessionService sessions,
            ActivityService activities,
            RegistrationService registrations,
            SessionAttendanceService attendance) {
        this.sessions = sessions;
        this.activities = activities;
        this.registrations = registrations;
        this.attendance = attendance;
    }

    @GetMapping("/activity-sessions")
    @ApiResponse(responseCode = "200", description = "OK")
    public List<ActivitySessionResponse> list(
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @RequestParam(required = false) Long floorAreaId) {
        return sessions.list(ActivitySessionService.target(floorAreaId), from, to, floorAreaId).stream()
                .map(ActivitySessionResponse::of)
                .toList();
    }

    @PostMapping("/activity-sessions")
    @ApiResponse(responseCode = "201", description = "OK")
    @ApiResponse(
            responseCode = "409",
            content =
                    @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = CareProblemBody.class)))
    public ResponseEntity<Void> create(
            @RequestHeader("Idempotency-Key") String key, @RequestBody ActivitySessionCreateWrite body) {
        if (body.activityId() == null) throw new InvalidValueException("Thiếu hoạt động");
        if (body.trip() != null)
            throw new BusinessException(
                    HttpStatus.SERVICE_UNAVAILABLE, "CARE_SOURCE_UNAVAILABLE", "Chuyến đi thuộc US4 (T016)");
        long id = sessions.createSingle(
                activities.target(body.activityId().longValue()),
                body.activityId(),
                body.startsAt(),
                body.endsAt(),
                body.locationId(),
                body.responsibleStaffId());
        return ResponseEntity.created(URI.create("/api/v1/activity-sessions/" + id))
                .build();
    }

    @GetMapping("/activity-sessions/{id}")
    @ApiResponse(responseCode = "200", description = "OK")
    public ActivitySessionDetailResponse detail(@PathVariable long id) {
        return ActivitySessionDetailResponse.of(sessions.detail(sessions.target(id), id));
    }

    @PostMapping("/activity-sessions/{id}/{command}")
    @ApiResponse(responseCode = "200", description = "OK")
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
                            schema = @Schema(allowableValues = {"reschedule", "cancel", "complete-attendance"}))
                    String command,
            @RequestHeader("Idempotency-Key") String key,
            @RequestBody(required = false) ActivitySessionCommandWrite body) {
        var target = sessions.target(id);
        var input = body == null ? new ActivitySessionCommandWrite(null, null, null, null, null) : body;
        var reason = input.reason() == null || input.reason().isBlank() ? null : new Reason(input.reason());
        switch (command) {
            case "reschedule" ->
                sessions.reschedule(
                        target,
                        id,
                        reason,
                        input.startsAt(),
                        input.endsAt(),
                        input.locationId(),
                        input.responsibleStaffId());
            case "cancel" -> sessions.cancel(target, id, reason);
            case "complete-attendance" -> attendance.complete(target, id);
            default -> throw new InvalidValueException("Lệnh không hợp lệ: " + command);
        }
    }

    /** 014 T012 (FR-015 → FR-018): 409 kèm điều kiện chưa thỏa hoặc cảnh báo cần xác nhận. */
    @PostMapping("/activity-sessions/{id}/registrations")
    @ApiResponse(responseCode = "201", description = "OK")
    @ApiResponse(
            responseCode = "409",
            content =
                    @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = CareProblemBody.class)))
    public ResponseEntity<Void> register(
            @PathVariable long id,
            @RequestHeader("Idempotency-Key") String key,
            @RequestBody ActivityRegistrationWrite body) {
        if (body.residentId() == null) throw new InvalidValueException("Thiếu người cao tuổi");
        long registration = registrations.register(
                registrations.target(id, body.residentId()),
                id,
                body.residentId(),
                Boolean.TRUE.equals(body.acknowledgeWarnings()));
        return ResponseEntity.created(URI.create("/api/v1/activity-registrations/" + registration))
                .build();
    }

    /** 014 T012 (FR-017): hủy trước giờ bắt đầu, lý do bắt buộc. */
    @PostMapping("/activity-registrations/{id}/cancel")
    @ApiResponse(responseCode = "200", description = "OK")
    @ApiResponse(
            responseCode = "409",
            content =
                    @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = CareProblemBody.class)))
    public void cancelRegistration(
            @PathVariable long id,
            @RequestHeader("Idempotency-Key") String key,
            @RequestBody ActivityReasonWrite body) {
        registrations.cancel(registrations.targetOfRegistration(id), id, body.reason());
    }

    /** 014 T014 (FR-027 → FR-032): một hoặc nhiều kết quả; thêm người lúc điểm danh. */
    @PostMapping("/activity-sessions/{id}/attendance")
    @ApiResponse(responseCode = "200", description = "OK")
    @ApiResponse(
            responseCode = "409",
            content =
                    @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = CareProblemBody.class)))
    public void attendance(
            @PathVariable long id,
            @RequestHeader("Idempotency-Key") String key,
            @RequestBody SessionAttendanceWrite body) {
        attendance.record(sessions.target(id), id, body.toItems(), body.deviceAt());
    }
}
