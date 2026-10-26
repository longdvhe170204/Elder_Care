package com.cambae.backend.m02.web;

import com.cambae.backend.common.error.InvalidValueException;
import com.cambae.backend.common.error.web.ProblemBody;
import com.cambae.backend.m02.internal.absence.service.AbsenceNames;
import com.cambae.backend.m02.internal.death.service.DeathRecordService;
import com.cambae.backend.m02.internal.death.service.PostDeathTaskService;
import com.cambae.backend.m02.internal.ending.service.ResidencyEndingService;
import com.cambae.backend.m02.web.request.CorrectionRequestBody;
import com.cambae.backend.m02.web.request.ReasonBody;
import com.cambae.backend.m02.web.response.ApprovalRequestRefBody;
import com.cambae.backend.m02.web.response.DeathRecordBody;
import com.cambae.backend.m02.web.response.PostDeathTaskListBody;
import com.cambae.backend.m15.correction.api.CorrectionApi;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
 * Qua đời (004 US8; contracts/api-004.yaml, tag {@code death}). Ghi nhận qua đời là lệnh {@code POST
 * /residents/{id}/record-death} của 001 nên không có endpoint ghi ở đây. Mọi logic và kiểm quyền ở service.
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "death")
class DeathController {

    private static final String PROBLEM = "application/problem+json";

    private final DeathRecordService records;
    private final PostDeathTaskService tasks;
    private final CorrectionApi corrections;
    private final AbsenceNames names;

    DeathController(
            DeathRecordService records, PostDeathTaskService tasks, CorrectionApi corrections, AbsenceNames names) {
        this.records = records;
        this.tasks = tasks;
        this.corrections = corrections;
        this.names = names;
    }

    @GetMapping("/residents/{residentId}/death-record")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(
                responseCode = "404",
                description = "Chưa có bản ghi qua đời",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class)))
    })
    DeathRecordBody get(@PathVariable long residentId) {
        return DeathRecordBody.of(records.get(DeathRecordService.targetForResident(residentId), residentId), names);
    }

    @PostMapping("/death-records/{id}/correct")
    @ApiResponses({@ApiResponse(responseCode = "201", description = "Đã đính chính hoặc đã lập yêu cầu")})
    ResponseEntity<Void> correct(
            @PathVariable long id,
            @RequestHeader(name = "Idempotency-Key", required = false) UUID idempotencyKey,
            @Valid @RequestBody CorrectionRequestBody body) {
        if (body.type() != CorrectionRequestBody.Type.EDIT_CONTENT) {
            throw new InvalidValueException(
                    "Bản ghi qua đời chỉ bổ sung hoặc sửa nội dung, không hủy được (FR-069); trạng thái Qua đời đã ghi ở"
                            + " 001");
        }
        records.targetOf(id);
        corrections.create(
                PostDeathTaskService.ENTITY_TYPE,
                id,
                body.type().correctionType(),
                body.correctedContent(),
                body.reason());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/residents/{residentId}/post-death-tasks")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "OK")})
    PostDeathTaskListBody postDeathTasks(@PathVariable long residentId) {
        return PostDeathTaskListBody.of(tasks.list(DeathRecordService.targetForResident(residentId), residentId));
    }

    @PostMapping("/residents/{residentId}/post-death-tasks/request-settlement-exception")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Đã lập yêu cầu"),
        @ApiResponse(
                responseCode = "403",
                description = "Không có quyền lập ngoại lệ mục quyết toán",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class))),
        @ApiResponse(
                responseCode = "409",
                description = "POST_DEATH_LIST_CLOSED, POST_DEATH_SETTLEMENT_NOT_OPEN",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class)))
    })
    ResponseEntity<ApprovalRequestRefBody> requestSettlementException(
            @PathVariable long residentId,
            @RequestHeader(name = "Idempotency-Key", required = false) UUID idempotencyKey,
            @Valid @RequestBody ReasonBody body) {
        var request = tasks.requestSettlementException(
                ResidencyEndingService.settlementExceptionTarget(residentId), residentId, body.reason());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApprovalRequestRefBody.of(request));
    }
}
