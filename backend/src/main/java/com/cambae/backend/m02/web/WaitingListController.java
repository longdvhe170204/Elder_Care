package com.cambae.backend.m02.web;

import com.cambae.backend.common.error.web.ProblemBody;
import com.cambae.backend.m02.internal.application.service.M02ActorNames;
import com.cambae.backend.m02.internal.waitinglist.domain.WaitingListStatus;
import com.cambae.backend.m02.internal.waitinglist.service.WaitingListService;
import com.cambae.backend.m02.web.request.AddToWaitingListBody;
import com.cambae.backend.m02.web.request.ProposeScoreAdjustmentBody;
import com.cambae.backend.m02.web.request.ReasonBody;
import com.cambae.backend.m02.web.request.UpdateCircumstancesBody;
import com.cambae.backend.m02.web.response.ApprovalRequestRefBody;
import com.cambae.backend.m02.web.response.ScoreHistoryEntryBody;
import com.cambae.backend.m02.web.response.WaitingListEntryBody;
import com.cambae.backend.m15.account.api.dto.Target;
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
 * Danh sách chờ (004 US2; contracts/api-004.yaml, tag {@code waiting-list}). Mọi logic và kiểm quyền ở service. Danh sách
 * hồ sơ phù hợp với giường, ghi nhận đã liên hệ, từ chối giường chờ 003 công bố giao diện nên chưa có ở đây.
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "waiting-list")
class WaitingListController {

    private static final String PROBLEM = "application/problem+json";

    private final WaitingListService waitingList;
    private final M02ActorNames names;

    WaitingListController(WaitingListService waitingList, M02ActorNames names) {
        this.waitingList = waitingList;
        this.names = names;
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
        var created = waitingList.create(target, body.applicationId(), body.circumstances());
        return ResponseEntity.status(HttpStatus.CREATED).body(WaitingListEntryBody.of(created));
    }

    @GetMapping("/waiting-list-entries/{id}")
    WaitingListEntryBody get(@PathVariable long id) {
        Target target = waitingList.targetOf(id);
        return WaitingListEntryBody.of(waitingList.get(target, id));
    }

    @GetMapping("/waiting-list-entries/{id}/score-history")
    List<ScoreHistoryEntryBody> scoreHistory(@PathVariable long id) {
        Target target = waitingList.targetOf(id);
        return waitingList.history(target, id).stream()
                .map(view -> ScoreHistoryEntryBody.of(view, names::of))
                .toList();
    }

    @PostMapping("/waiting-list-entries/{id}/update-circumstances")
    WaitingListEntryBody updateCircumstances(
            @PathVariable long id,
            @RequestHeader(name = "Idempotency-Key", required = false) UUID idempotencyKey,
            @Valid @RequestBody UpdateCircumstancesBody body) {
        Target target = waitingList.targetOf(id);
        return WaitingListEntryBody.of(
                waitingList.updateCircumstances(target, id, body.circumstances(), body.reason()));
    }

    @PostMapping("/waiting-list-entries/{id}/propose-score-adjustment")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Đã lập yêu cầu"),
        @ApiResponse(
                responseCode = "422",
                description = "Lý do bắt buộc, points khác 0",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class)))
    })
    ResponseEntity<ApprovalRequestRefBody> proposeScoreAdjustment(
            @PathVariable long id,
            @RequestHeader(name = "Idempotency-Key", required = false) UUID idempotencyKey,
            @Valid @RequestBody ProposeScoreAdjustmentBody body) {
        Target target = waitingList.targetOf(id);
        var request = waitingList.proposeAdjustment(target, id, body.points(), body.reason());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApprovalRequestRefBody.of(request));
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
