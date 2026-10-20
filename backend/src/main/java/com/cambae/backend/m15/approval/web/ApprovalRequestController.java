package com.cambae.backend.m15.approval.web;

import com.cambae.backend.common.Reason;
import com.cambae.backend.common.error.web.ProblemBody;
import com.cambae.backend.common.web.VersionEtag;
import com.cambae.backend.m15.account.api.AccountApi;
import com.cambae.backend.m15.approval.api.ApprovalRequestView;
import com.cambae.backend.m15.approval.api.CreateRequest;
import com.cambae.backend.m15.approval.api.ResultRef;
import com.cambae.backend.m15.approval.internal.ApprovalRequestService;
import com.cambae.backend.m15.approval.internal.ApprovalRequestService.Inbox;
import com.cambae.backend.m15.approval.internal.ApprovalRequestService.Page;
import com.cambae.backend.m15.approval.internal.ApprovalRequestService.Viewed;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.function.LongFunction;
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
import tools.jackson.databind.JsonNode;

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

    private ResponseEntity<ApprovalRequestBody> withEtag(HttpStatus status, Viewed viewed) {
        return ResponseEntity.status(status)
                .eTag(VersionEtag.of(viewed.request().version()))
                .body(ApprovalRequestBody.of(viewed, this::fullName));
    }

    public record CreateRequestBody(
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
            String type,

            @Schema(requiredMode = Schema.RequiredMode.REQUIRED, implementation = Object.class)
            JsonNode content,

            @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
            LocalDate requestedEffectiveDate,

            @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
            Reason reason,

            Boolean submitImmediately) {}

    public record EditContentBody(
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED, implementation = Object.class)
            JsonNode content,

            LocalDate requestedEffectiveDate,

            @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
            Reason reason) {}

    public record ApproveBody(@Schema(maxLength = 2000) String comment) {}

    public record RejectBody(
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
            Reason comment) {}

    public record CancelBody(
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
            Reason reason) {}

    public record ApprovalRequestPageBody(List<ApprovalRequestBody> items, String nextCursor) {}

    /** Họ tên theo hồ sơ tài khoản (002 T032a); không có thì {@code null}. */
    String fullName(long accountId) {
        return accounts.getObject().fullName(accountId).orElse(null);
    }

    /** Người thực hiện là người dùng hay Bộ lập lịch (schema {@code Actor}). */
    public enum ActorType {
        USER,
        SCHEDULER,
        // Công cụ khởi tạo Quản lý viện đầu tiên (002 BR-M15-22); chỉ nhật ký có
        BOOTSTRAP,
        // Lần đăng nhập thất bại, chưa xác định người (002 FR-015); chỉ nhật ký có
        ANONYMOUS
    }

    /** Người thực hiện theo schema {@code Actor}; họ tên theo hồ sơ tài khoản (002 T032a). */
    public record ActorBody(ActorType type, Long id, String fullName, String role) {

        static ActorBody user(Long id, LongFunction<String> names) {
            return id == null ? null : new ActorBody(ActorType.USER, id, names.apply(id), null);
        }
    }

    /** Yêu cầu theo schema {@code ApprovalRequest}. */
    public record ApprovalRequestBody(
            long id,
            String displayCode,
            String type,

            @Schema(
                    allowableValues = {
                        "DRAFT",
                        "PENDING_APPROVAL",
                        "APPROVED",
                        "APPLIED",
                        "APPLY_FAILED",
                        "REJECTED",
                        "CANCELLED"
                    })
            String status,

            @Schema(implementation = Object.class) JsonNode content,
            ActorBody requester,
            Instant createdAt,
            String reason,
            LocalDate requestedEffectiveDate,
            LocalDate actualEffectiveDate,
            Instant submittedAt,
            ActorBody decidedBy,
            Instant decidedAt,
            String decisionComment,
            boolean selfApproved,
            String closingReason,
            Instant appliedAt,
            boolean catchUpApplied,
            ResultRef applicationResult,
            List<String> allowedCommands,
            int version) {

        public static ApprovalRequestBody of(Viewed viewed, LongFunction<String> names) {
            ApprovalRequestView r = viewed.request();
            return new ApprovalRequestBody(
                    r.id(),
                    r.displayCode(),
                    r.type(),
                    r.status(),
                    r.content(),
                    ActorBody.user(r.requesterId(), names),
                    r.createdAt(),
                    r.reason(),
                    r.requestedEffectiveDate(),
                    r.actualEffectiveDate(),
                    r.submittedAt(),
                    ActorBody.user(r.decidedBy(), names),
                    r.decidedAt(),
                    r.decisionComment(),
                    r.selfApproved(),
                    r.closingReason(),
                    r.appliedAt(),
                    r.catchUpApplied(),
                    r.applicationResult(),
                    viewed.allowedCommands(),
                    r.version());
        }
    }
}
