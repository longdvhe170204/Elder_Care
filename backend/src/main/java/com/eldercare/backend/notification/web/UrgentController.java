package com.eldercare.backend.notification.web;

import com.eldercare.backend.common.error.web.ProblemBody;
import com.eldercare.backend.notification.internal.urgent.service.CallReminderQueries;
import com.eldercare.backend.notification.internal.urgent.service.CallResultService;
import com.eldercare.backend.notification.internal.urgent.service.CallResultService.CallResultInput;
import com.eldercare.backend.notification.internal.urgent.service.UrgentAcknowledgement;
import com.eldercare.backend.notification.web.request.CallResultBody;
import com.eldercare.backend.notification.web.response.CallReminderBody;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
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
 * Thông báo Khẩn cấp theo contracts/api-009.yaml (thẻ {@code urgent}; 009 T021): xác nhận đã nhận, lượt nhắc gọi của tôi,
 * chi tiết lượt, ghi kết quả gọi. "Xác nhận và tiếp nhận" chờ lệnh tiếp nhận của 007.
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "urgent")
public class UrgentController {

    private final UrgentAcknowledgement acknowledgement;
    private final CallReminderQueries reminders;
    private final CallResultService results;

    public UrgentController(
            UrgentAcknowledgement acknowledgement, CallReminderQueries reminders, CallResultService results) {
        this.acknowledgement = acknowledgement;
        this.reminders = reminders;
        this.results = results;
    }

    @PostMapping("/notifications/{id}/acknowledge")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK", content = @Content),
        @ApiResponse(
                responseCode = "404",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = ProblemBody.class))),
        @ApiResponse(
                responseCode = "409",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = ProblemBody.class)))
    })
    ResponseEntity<Void> acknowledge(
            @PathVariable long id, @RequestHeader(name = "Idempotency-Key", required = false) UUID idempotencyKey) {
        acknowledgement.acknowledge(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/me/call-reminders")
    List<CallReminderBody> myCallReminders() {
        return reminders.mine().stream().map(CallReminderBody::of).toList();
    }

    @GetMapping("/call-reminders/{id}")
    CallReminderBody callReminder(@PathVariable long id) {
        return CallReminderBody.of(reminders.detail(id));
    }

    @PostMapping("/call-reminders/{id}/results")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "OK", content = @Content),
        @ApiResponse(
                responseCode = "403",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = ProblemBody.class))),
        @ApiResponse(
                responseCode = "409",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = ProblemBody.class))),
        @ApiResponse(
                responseCode = "422",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = ProblemBody.class)))
    })
    ResponseEntity<Void> recordCallResult(
            @PathVariable long id,
            @RequestHeader(name = "Idempotency-Key", required = false) UUID idempotencyKey,
            @Valid @RequestBody CallResultBody body) {
        results.record(
                id,
                new CallResultInput(
                        body.result().name(),
                        body.calledAt().toInstant(),
                        Optional.ofNullable(body.calledPhoneNumber()),
                        Optional.ofNullable(body.reason()).map(Enum::name),
                        Optional.ofNullable(body.answeredBy()),
                        Optional.ofNullable(body.note())));
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
