package com.eldercare.backend.administration.approval.web;

import com.eldercare.backend.administration.account.api.AccountApi;
import com.eldercare.backend.administration.approval.api.ApprovalRequestDetails;
import com.eldercare.backend.administration.approval.api.ApprovalRequestView;
import com.eldercare.backend.administration.approval.api.CreateRequest;
import com.eldercare.backend.administration.approval.api.dto.ApprovalRequestBody;
import com.eldercare.backend.administration.approval.internal.service.ApprovalRequestService;
import com.eldercare.backend.administration.approval.internal.service.ApprovalRequestService.Inbox;
import com.eldercare.backend.administration.approval.internal.service.ApprovalRequestService.Page;
import com.eldercare.backend.administration.approval.web.request.ApproveBody;
import com.eldercare.backend.administration.approval.web.request.CancelBody;
import com.eldercare.backend.administration.approval.web.request.CreateRequestBody;
import com.eldercare.backend.administration.approval.web.request.EditContentBody;
import com.eldercare.backend.administration.approval.web.request.RejectBody;
import com.eldercare.backend.administration.approval.web.response.ApprovalRequestPageBody;
import com.eldercare.backend.common.error.web.ProblemBody;
import com.eldercare.backend.common.web.VersionEtag;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
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
 * {@code /api/v1/approval-requests} theo contracts/api-000.yaml (000 T095). Chi tiết và kết quả lệnh kèm
 * {@code allowedCommands}, tính cùng điều kiện quyền, phạm vi, giới hạn vai trò như chính lệnh (FR-034).
 */
@RestController
@RequestMapping("/api/v1/approval-requests")
public class ApprovalRequestController {

    private final ApprovalRequestService requests;
    private final ObjectProvider<AccountApi> accounts;

    public ApprovalRequestController(ApprovalRequestService requests, ObjectProvider<AccountApi> accounts) {
        this.requests = requests;
        this.accounts = accounts;
    }

    @PostMapping
    @ApiResponses({
        @ApiResponse(
                responseCode = "201",
                content =
                        @Content(
                                mediaType = "application/json",
                                schema = @Schema(implementation = ApprovalRequestBody.class))),
        @ApiResponse(
                responseCode = "403",
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
    public ResponseEntity<ApprovalRequestBody> create(
            @RequestHeader(name = "Idempotency-Key", required = false) UUID idempotencyKey,
            @RequestBody CreateRequestBody body) {
        ApprovalRequestView created = requests.create(new CreateRequest(
                body.type(),
                body.content(),
                body.requestedEffectiveDate(),
                body.reason(),
                Boolean.TRUE.equals(body.submitImmediately())));
        return withEtag(HttpStatus.CREATED, requests.detail(created.id()));
    }

    @GetMapping
    @ApiResponses({@ApiResponse(responseCode = "200", description = "OK")})
    public ApprovalRequestPageBody list(
            @RequestParam(required = false) Inbox inbox,
            @RequestParam(required = false) List<String> status,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String cursor) {
        Page page = requests.list(inbox, status, type, cursor);
        return new ApprovalRequestPageBody(
                page.items().stream()
                        .map(v -> ApprovalRequestBody.of(v, this::fullName))
                        .toList(),
                page.nextCursor());
    }

    @GetMapping("/{id}")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(
                responseCode = "404",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = ProblemBody.class)))
    })
    public ResponseEntity<ApprovalRequestBody> detail(@PathVariable long id) {
        return withEtag(HttpStatus.OK, requests.detail(id));
    }

    @PostMapping("/{id}/edit-content")
    @CommandResponses
    public ResponseEntity<ApprovalRequestBody> editContent(
            @PathVariable long id, @RequestHeader("If-Match") String ifMatch, @RequestBody EditContentBody body) {
        requests.editContent(id, ifMatch, body.content(), body.requestedEffectiveDate(), body.reason());
        return withEtag(HttpStatus.OK, requests.detail(id));
    }

    @PostMapping("/{id}/submit")
    @CommandResponses
    public ResponseEntity<ApprovalRequestBody> submit(
            @PathVariable long id, @RequestHeader("If-Match") String ifMatch) {
        requests.submit(id, ifMatch);
        return withEtag(HttpStatus.OK, requests.detail(id));
    }

    @PostMapping("/{id}/approve")
    @CommandResponses
    public ResponseEntity<ApprovalRequestBody> approve(
            @PathVariable long id,
            @RequestHeader("If-Match") String ifMatch,
            @RequestBody(required = false) ApproveBody body) {
        requests.approve(id, ifMatch, body == null ? null : body.comment());
        return withEtag(HttpStatus.OK, requests.detail(id));
    }

    @PostMapping("/{id}/reject")
    @CommandResponses
    public ResponseEntity<ApprovalRequestBody> reject(
            @PathVariable long id, @RequestHeader("If-Match") String ifMatch, @RequestBody RejectBody body) {
        requests.reject(id, ifMatch, body.comment());
        return withEtag(HttpStatus.OK, requests.detail(id));
    }

    @PostMapping("/{id}/cancel")
    @CommandResponses
    public ResponseEntity<ApprovalRequestBody> cancel(
            @PathVariable long id, @RequestHeader("If-Match") String ifMatch, @RequestBody CancelBody body) {
        requests.cancel(id, ifMatch, body.reason());
        return withEtag(HttpStatus.OK, requests.detail(id));
    }

    private ResponseEntity<ApprovalRequestBody> withEtag(HttpStatus status, ApprovalRequestDetails viewed) {
        return ResponseEntity.status(status)
                .eTag(VersionEtag.of(viewed.request().version()))
                .body(ApprovalRequestBody.of(viewed, this::fullName));
    }

    /** Họ tên theo hồ sơ tài khoản (002 T032a); không có thì {@code null}. */
    String fullName(long accountId) {
        return accounts.getObject().fullName(accountId).orElse(null);
    }
}
