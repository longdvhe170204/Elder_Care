package com.eldercare.backend.belongings.web;

import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.belongings.internal.belonging.service.BelongingQueries;
import com.eldercare.backend.belongings.internal.returns.domain.RecipientConfirmation;
import com.eldercare.backend.belongings.internal.returns.service.RecipientConfirmationService;
import com.eldercare.backend.belongings.internal.returns.service.ReturnService;
import com.eldercare.backend.belongings.web.request.ReturnRequests;
import com.eldercare.backend.belongings.web.response.HandoverResponse;
import com.eldercare.backend.belongings.web.response.IdResponse;
import com.eldercare.backend.belongings.web.response.RecipientConfirmationResponse;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.error.NotFoundException;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Trả đồ gửi và xác nhận người nhận khác (013 T019; api-013 tag {@code belonging-returns}, {@code family-portal}): Hành
 * chính, Điều dưỡng trả; Hành chính lập, hủy, ghi bằng chứng liên hệ; Quản lý viện duyệt thay; người đại diện xác nhận,
 * từ chối qua cổng (không {@code @RequiresPermission}: phạm vi "quan hệ" của 002; service kiểm tài khoản người thân và
 * người đại diện, như đồng ý cho tự giữ).
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "belonging-returns")
class ReturnController {

    private static final String ENTITY = "M12.BELONGING";

    private static final Set<String> STATUSES = Set.of(
            RecipientConfirmation.PENDING_CONFIRMATION,
            RecipientConfirmation.EFFECTIVE,
            RecipientConfirmation.REJECTED,
            RecipientConfirmation.CANCELLED,
            RecipientConfirmation.USED,
            RecipientConfirmation.EXPIRED);

    private final ReturnService returns;
    private final RecipientConfirmationService confirmations;
    private final BelongingQueries queries;

    ReturnController(ReturnService returns, RecipientConfirmationService confirmations, BelongingQueries queries) {
        this.returns = returns;
        this.confirmations = confirmations;
        this.queries = queries;
    }

    @PostMapping("/belongings/returns")
    @ApiResponse(responseCode = "201", description = "OK", content = @Content)
    @ResponseStatus(HttpStatus.CREATED)
    HandoverResponse giveBack(
            @RequestHeader(name = "Idempotency-Key", required = false) UUID idempotencyKey,
            @RequestBody ReturnRequests.Return body) {
        var command = body.toCommand();
        if (command == null) {
            throw invalid("Thiếu đồ, tình trạng lúc trả hoặc người nhận");
        }
        long residentId = queries.residentOf(body.belongingIds());
        return HandoverResponse.of(returns.giveBack(new Target(ENTITY, null, residentId, null), command));
    }

    @GetMapping("/recipient-confirmations")
    List<RecipientConfirmationResponse> list(
            @RequestParam(name = "residentId", required = false) Long residentId,
            @RequestParam(name = "status", required = false) String status) {
        Set<String> statuses = status == null || status.isBlank()
                ? Set.of()
                : Arrays.stream(status.split(",")).map(String::strip).collect(Collectors.toSet());
        if (!STATUSES.containsAll(statuses)) {
            throw invalid("Trạng thái không hợp lệ");
        }
        var views = residentId == null
                ? confirmations.listAll(statuses)
                : confirmations.list(new Target(ENTITY, null, residentId, null), statuses);
        return views.stream().map(RecipientConfirmationResponse::of).toList();
    }

    @PostMapping("/recipient-confirmations")
    @ApiResponse(responseCode = "201", description = "OK", content = @Content)
    @ResponseStatus(HttpStatus.CREATED)
    IdResponse create(
            @RequestHeader(name = "Idempotency-Key", required = false) UUID idempotencyKey,
            @RequestBody ReturnRequests.CreateConfirmation body) {
        if (body.residentId() == null) {
            throw invalid("Thiếu người cao tuổi");
        }
        return new IdResponse(
                confirmations.create(new Target(ENTITY, null, body.residentId(), null), body.toCommand()));
    }

    @PostMapping("/recipient-confirmations/{id}/contact-evidence")
    @ResponseStatus(HttpStatus.CREATED)
    void contactEvidence(
            @PathVariable long id,
            @RequestHeader(name = "Idempotency-Key", required = false) UUID idempotencyKey,
            @RequestBody ReturnRequests.ContactAttempt body) {
        confirmations.recordContact(target(id), id, body.toCommand());
    }

    @PostMapping("/recipient-confirmations/{id}/{command}")
    void command(
            @PathVariable long id,
            @PathVariable @Schema(allowableValues = {"approve-on-behalf", "cancel"}) String command,
            @RequestHeader(name = "Idempotency-Key", required = false) UUID idempotencyKey,
            @RequestBody ReturnRequests.Command body) {
        switch (command) {
            case "approve-on-behalf" -> confirmations.approveOnBehalf(target(id), id, body.reason(), body.basis());
            case "cancel" -> confirmations.cancel(target(id), id, body.reason());
            default -> throw new NotFoundException();
        }
    }

    @PostMapping("/family-portal/recipient-confirmations/{id}/{command}")
    void portalCommand(
            @PathVariable long id,
            @PathVariable @Schema(allowableValues = {"confirm", "reject"}) String command,
            @RequestHeader(name = "Idempotency-Key", required = false) UUID idempotencyKey,
            @RequestBody(required = false) ReturnRequests.PortalCommand body) {
        switch (command) {
            case "confirm" -> confirmations.confirmOnPortal(id);
            case "reject" -> confirmations.rejectOnPortal(id, body == null ? null : body.reason());
            default -> throw new NotFoundException();
        }
    }

    private Target target(long confirmationId) {
        return new Target(ENTITY, null, confirmations.residentOf(confirmationId), null);
    }

    private static BusinessException invalid(String message) {
        return new BusinessException(HttpStatus.UNPROCESSABLE_CONTENT, "INVALID_VALUE", message);
    }
}
