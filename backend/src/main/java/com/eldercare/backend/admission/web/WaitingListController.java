package com.eldercare.backend.admission.web;

import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.admission.internal.waitinglist.domain.WaitingListStatus;
import com.eldercare.backend.admission.internal.waitinglist.service.WaitingListService;
import com.eldercare.backend.admission.web.request.AddToWaitingListBody;
import com.eldercare.backend.admission.web.request.MarkPriorityBody;
import com.eldercare.backend.admission.web.request.ReasonBody;
import com.eldercare.backend.admission.web.request.RecordContactedBody;
import com.eldercare.backend.admission.web.response.MatchingEntriesBody;
import com.eldercare.backend.admission.web.response.WaitingListEntryBody;
import com.eldercare.backend.common.error.web.ProblemBody;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Danh sách chờ (004 US2; contracts/api-004.yaml, tag {@code waiting-list}). Mọi logic và kiểm quyền ở service.
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "waiting-list")
class WaitingListController {

    private static final String PROBLEM = "application/problem+json";

    private final WaitingListService waitingList;

    WaitingListController(WaitingListService waitingList) {
        this.waitingList = waitingList;
    }

    @GetMapping("/waiting-list-entries")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(
                responseCode = "403",
                description = "Không có quyền",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class)))
    })
    List<WaitingListEntryBody> list(@RequestParam(name = "status", required = false) Set<WaitingListStatus> status) {
        return waitingList.list(Target.UNSPECIFIED, status).stream()
                .map(WaitingListEntryBody::of)
                .toList();
    }

    @PostMapping("/waiting-list-entries")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Đã tạo"),
        @ApiResponse(
                responseCode = "409",
                description =
                        "APPLICATION_NOT_IN_PROGRESS, RESIDENT_NOT_ADMISSION_IN_PROGRESS, WAITING_ENTRY_ALREADY_EXISTS",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class)))
    })
    ResponseEntity<WaitingListEntryBody> create(
            @RequestHeader(name = "Idempotency-Key", required = false) UUID idempotencyKey,
            @Valid @RequestBody AddToWaitingListBody body) {
        Target target = waitingList.targetOfApplication(body.applicationId());
        var created = waitingList.create(target, body.applicationId());
        return ResponseEntity.status(HttpStatus.CREATED).body(WaitingListEntryBody.of(created));
    }

    @GetMapping("/waiting-list-entries/{id}")
    WaitingListEntryBody get(@PathVariable long id) {
        Target target = waitingList.targetOf(id);
        return WaitingListEntryBody.of(waitingList.get(target, id));
    }

    @PostMapping("/waiting-list-entries/{id}/mark-priority")
    WaitingListEntryBody markPriority(
            @PathVariable long id,
            @RequestHeader(name = "Idempotency-Key", required = false) UUID idempotencyKey,
            @Valid @RequestBody MarkPriorityBody body) {
        Target target = waitingList.targetOf(id);
        return WaitingListEntryBody.of(waitingList.markPriority(target, id, body.priority(), body.reason()));
    }

    @GetMapping("/beds/{bedId}/matching-entries")
    MatchingEntriesBody matching(@PathVariable long bedId) {
        return MatchingEntriesBody.of(waitingList.matchingOf(Target.UNSPECIFIED, bedId));
    }

    @PostMapping("/waiting-list-entries/{id}/record-contacted")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(
                responseCode = "409",
                description = "WAITING_ENTRY_NOT_WAITING, BED_NOT_EMPTY, ENTRY_NOT_MATCHING_BED, BED_ALREADY_HELD",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class))),
        @ApiResponse(
                responseCode = "422",
                description = "Thiếu lý do khi chọn không theo thứ tự",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class)))
    })
    WaitingListEntryBody recordContacted(
            @PathVariable long id,
            @RequestHeader(name = "Idempotency-Key", required = false) UUID idempotencyKey,
            @Valid @RequestBody RecordContactedBody body) {
        Target target = waitingList.targetOf(id);
        return WaitingListEntryBody.of(waitingList.recordContacted(target, id, body.bedId(), body.outOfOrderReason()));
    }

    @PostMapping("/waiting-list-entries/{id}/decline-bed")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK", content = @Content),
        @ApiResponse(
                responseCode = "409",
                description = "WAITING_ENTRY_NOT_CONTACTED",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class)))
    })
    ResponseEntity<Void> declineBed(
            @PathVariable long id,
            @RequestHeader(name = "Idempotency-Key", required = false) UUID idempotencyKey,
            @Valid @RequestBody ReasonBody body) {
        Target target = waitingList.targetOf(id);
        waitingList.declineBed(target, id, body.reason());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/waiting-list-entries/{id}/decline")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK", content = @Content),
        @ApiResponse(
                responseCode = "409",
                description = "WAITING_ENTRY_CLOSED, điều kiện của lệnh Hủy tiếp nhận (001)",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class)))
    })
    ResponseEntity<Void> decline(
            @PathVariable long id,
            @RequestHeader(name = "Idempotency-Key", required = false) UUID idempotencyKey,
            @Valid @RequestBody ReasonBody body) {
        Target target = waitingList.targetOf(id);
        waitingList.decline(target, id, body.reason());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/waiting-list-entries/{id}/cancel-waiting")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK", content = @Content),
        @ApiResponse(
                responseCode = "409",
                description = "WAITING_ENTRY_CLOSED, điều kiện của lệnh Hủy tiếp nhận (001)",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class)))
    })
    ResponseEntity<Void> cancelWaiting(
            @PathVariable long id,
            @RequestHeader(name = "Idempotency-Key", required = false) UUID idempotencyKey,
            @Valid @RequestBody ReasonBody body) {
        Target target = waitingList.targetOf(id);
        waitingList.cancelWaiting(target, id, body.reason());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/waiting-list-entries/{id}/confirm-still-needed")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "OK", content = @Content)})
    ResponseEntity<Void> confirmStillNeeded(
            @PathVariable long id, @RequestHeader(name = "Idempotency-Key", required = false) UUID idempotencyKey) {
        Target target = waitingList.targetOf(id);
        waitingList.confirmStillNeeded(target, id);
        return ResponseEntity.ok().build();
    }
}
