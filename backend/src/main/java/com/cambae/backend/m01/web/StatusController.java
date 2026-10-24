package com.cambae.backend.m01.web;

import com.cambae.backend.common.error.BusinessException;
import com.cambae.backend.common.error.web.ProblemBody;
import com.cambae.backend.m01.api.dto.Condition;
import com.cambae.backend.m01.api.dto.StatusCommand;
import com.cambae.backend.m01.internal.status.service.ResidentStatusService;
import com.cambae.backend.m01.internal.status.service.ResidentStatusService.Outcome;
import com.cambae.backend.m01.web.request.StatusCommandRequestBody;
import com.cambae.backend.m01.web.response.StatusCommandResultBody;
import com.cambae.backend.m01.web.response.StatusHistoryEntryBody;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.JsonNodeFactory;

/**
 * Lệnh và lịch sử trạng thái người cao tuổi (001 T050; contracts/api-001.yaml, tag {@code status}). Mỗi lệnh một
 * endpoint (constitution XI); mọi logic ở {@link ResidentStatusService}.
 */
@RestController
@RequestMapping("/api/v1/residents/{id}")
@Tag(name = "status")
class StatusController {

    private static final String PROBLEM = "application/problem+json";

    /** Lệnh người dùng được xem trước điều kiện (schema {@code UserCommand}). */
    private static final Set<StatusCommand> USER_COMMANDS = EnumSet.of(
            StatusCommand.COMPLETE_ADMISSION,
            StatusCommand.CANCEL_ADMISSION,
            StatusCommand.GRANT_TEMPORARY_ABSENCE,
            StatusCommand.RECORD_RETURN,
            StatusCommand.TRANSFER_OUT,
            StatusCommand.END_RESIDENCY,
            StatusCommand.RECORD_DEATH);

    private final ResidentStatusService status;

    StatusController(ResidentStatusService status) {
        this.status = status;
    }

    @GetMapping("/status-history")
    List<StatusHistoryEntryBody> history(@PathVariable long id) {
        return status.history(ResidentStatusService.target(id)).stream()
                .map(StatusHistoryEntryBody::of)
                .toList();
    }

    @GetMapping("/conditions/{command}")
    List<Condition> conditions(@PathVariable long id, @PathVariable StatusCommand command) {
        if (!USER_COMMANDS.contains(command)) {
            throw new BusinessException(
                    HttpStatus.UNPROCESSABLE_CONTENT,
                    "INVALID_COMMAND",
                    "Lệnh không có trong danh sách lệnh người dùng");
        }
        return status.previewConditions(ResidentStatusService.target(id), command);
    }

    @PostMapping("/complete-admission")
    @ApiResponses(
            @ApiResponse(
                    responseCode = "409",
                    description = "UNMET_CONDITIONS, INVALID_TRANSITION, PROFILE_CLOSED",
                    content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class))))
    StatusCommandResultBody completeAdmission(
            @PathVariable long id,
            @RequestHeader(name = "Idempotency-Key", required = false) UUID idempotencyKey,
            @Valid @RequestBody StatusCommandRequestBody body) {
        requireNoAttachments(body);
        return result(status.completeAdmission(ResidentStatusService.target(id), body.reason(), details(body)));
    }

    @PostMapping("/cancel-admission")
    StatusCommandResultBody cancelAdmission(
            @PathVariable long id,
            @RequestHeader(name = "Idempotency-Key", required = false) UUID idempotencyKey,
            @Valid @RequestBody StatusCommandRequestBody body) {
        requireNoAttachments(body);
        return result(status.cancelAdmission(ResidentStatusService.target(id), body.reason(), details(body)));
    }

    @PostMapping("/grant-temporary-absence")
    StatusCommandResultBody grantTemporaryAbsence(
            @PathVariable long id,
            @RequestHeader(name = "Idempotency-Key", required = false) UUID idempotencyKey,
            @Valid @RequestBody StatusCommandRequestBody body) {
        requireNoAttachments(body);
        return result(status.grantTemporaryAbsence(ResidentStatusService.target(id), body.reason(), details(body)));
    }

    @PostMapping("/record-return")
    StatusCommandResultBody recordReturn(
            @PathVariable long id,
            @RequestHeader(name = "Idempotency-Key", required = false) UUID idempotencyKey,
            @Valid @RequestBody StatusCommandRequestBody body) {
        requireNoAttachments(body);
        return result(status.recordReturn(ResidentStatusService.target(id), body.reason(), details(body)));
    }

    @PostMapping("/transfer-out")
    StatusCommandResultBody transferOut(
            @PathVariable long id,
            @RequestHeader(name = "Idempotency-Key", required = false) UUID idempotencyKey,
            @Valid @RequestBody StatusCommandRequestBody body) {
        requireNoAttachments(body);
        return result(status.transferOut(ResidentStatusService.target(id), body.reason(), details(body)));
    }

    @PostMapping("/end-residency")
    StatusCommandResultBody endResidency(
            @PathVariable long id,
            @RequestHeader(name = "Idempotency-Key", required = false) UUID idempotencyKey,
            @Valid @RequestBody StatusCommandRequestBody body) {
        requireNoAttachments(body);
        return result(status.endResidency(ResidentStatusService.target(id), body.reason(), details(body)));
    }

    @PostMapping("/record-death")
    StatusCommandResultBody recordDeath(
            @PathVariable long id,
            @RequestHeader(name = "Idempotency-Key", required = false) UUID idempotencyKey,
            @Valid @RequestBody StatusCommandRequestBody body) {
        requireNoAttachments(body);
        return result(status.recordDeath(ResidentStatusService.target(id), body.reason(), details(body)));
    }

    private static StatusCommandResultBody result(Outcome outcome) {
        return StatusCommandResultBody.of(outcome);
    }

    private static JsonNode details(StatusCommandRequestBody body) {
        return body.details() == null ? JsonNodeFactory.instance.objectNode() : body.details();
    }

    /**
     * Tệp đính kèm của Ghi nhận trở về từ bệnh viện và Chuyển viện thuộc T082 (cần {@code FileApi}, T003); tới đó mọi
     * lệnh từ chối tệp đính kèm thay vì bỏ qua tệp người dùng gửi.
     */
    private static void requireNoAttachments(StatusCommandRequestBody body) {
        if (body.attachmentFileIds() != null && !body.attachmentFileIds().isEmpty()) {
            throw new BusinessException(
                    HttpStatus.UNPROCESSABLE_CONTENT, "ATTACHMENTS_NOT_ACCEPTED", "Lệnh này chưa nhận tệp đính kèm");
        }
    }
}
