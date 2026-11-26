package com.eldercare.backend.care.web;

import com.eldercare.backend.administration.account.api.*;
import com.eldercare.backend.administration.account.api.dto.*;
import com.eldercare.backend.care.api.dto.CarePlanStatus;
import com.eldercare.backend.care.internal.careplan.domain.CarePlan;
import com.eldercare.backend.care.internal.careplan.service.*;
import com.eldercare.backend.care.web.request.*;
import com.eldercare.backend.care.web.response.*;
import com.eldercare.backend.care.web.response.CareProblemBody;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.common.web.VersionEtag;
import com.eldercare.backend.resident.api.ResidentApi;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** T016: các lệnh phiên bản dùng service riêng để aspect quyền/transaction luôn chạy. */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "ke-hoach")
public class CarePlanController {
    private final CarePlanService service;
    private final CarePlanQueries queries;
    private final AccountApi accounts;
    private final PermissionApi permissions;
    private final CurrentUser user;
    private final CareRiskService risks;
    private final BusinessTime time;
    private final CarePlanReviews reviews;
    private final ResidentApi residentApi;

    public CarePlanController(
            CarePlanService service,
            CarePlanQueries queries,
            AccountApi accounts,
            PermissionApi permissions,
            CurrentUser user,
            CareRiskService risks,
            BusinessTime time,
            CarePlanReviews reviews,
            ResidentApi residentApi) {
        this.reviews = reviews;
        this.residentApi = residentApi;
        this.service = service;
        this.queries = queries;
        this.accounts = accounts;
        this.permissions = permissions;
        this.user = user;
        this.risks = risks;
        this.time = time;
    }

    @PostMapping("/residents/{residentId}/care-plans")
    @ApiResponse(
            responseCode = "409",
            content =
                    @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = CareProblemBody.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "201",
            description = "Đã lập phiên bản Nháp",
            headers =
                    @io.swagger.v3.oas.annotations.headers.Header(
                            name = "ETag",
                            schema = @io.swagger.v3.oas.annotations.media.Schema(type = "string")))
    public ResponseEntity<CarePlanResponse> create(
            @PathVariable long residentId,
            @RequestHeader("Idempotency-Key") String key,
            @RequestBody CarePlanCreate body) {
        // Q-447: luôn chép bản Chờ hiệu lực hoặc Hiệu lực; copyCurrent giữ trong hợp đồng cho client cũ, bị bỏ qua.
        var row = service.create(CarePlanService.residentTarget(residentId, 0), residentId);
        return ResponseEntity.status(201).eTag(VersionEtag.of(row.version())).body(response(row));
    }

    @GetMapping("/residents/{residentId}/care-plans")
    @ApiResponse(responseCode = "200", description = "OK")
    @ApiResponse(
            responseCode = "403",
            content =
                    @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = CareProblemBody.class)))
    public List<CarePlanResponse> list(@PathVariable long residentId) {
        return queries.list(CarePlanService.residentTarget(residentId, 0), residentId).stream()
                .map(this::response)
                .toList();
    }

    @GetMapping("/care-plans/{id}")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Phiên bản kế hoạch",
            headers =
                    @io.swagger.v3.oas.annotations.headers.Header(
                            name = "ETag",
                            schema = @io.swagger.v3.oas.annotations.media.Schema(type = "string")))
    public ResponseEntity<CarePlanResponse> get(@PathVariable long id) {
        var row = queries.get(service.target(id), id);
        return ResponseEntity.ok().eTag(VersionEtag.of(row.version())).body(response(row));
    }

    @PutMapping("/care-plans/{id}/items")
    @ApiResponse(responseCode = "200", description = "OK")
    @ApiResponse(
            responseCode = "409",
            content =
                    @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = CareProblemBody.class)))
    @ApiResponse(
            responseCode = "412",
            content =
                    @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = CareProblemBody.class)))
    @ApiResponse(
            responseCode = "422",
            content =
                    @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = CareProblemBody.class)))
    public void items(
            @PathVariable long id,
            @RequestHeader("If-Match") String etag,
            @RequestHeader("Idempotency-Key") String key,
            @RequestBody List<CarePlanItemWrite> body) {
        requireRows(body);
        service.replaceItems(
                service.target(id),
                id,
                etag,
                body.stream().map(item -> item.item(id)).toList());
    }

    @PostMapping("/care-plans/{id}/submit")
    @ApiResponse(responseCode = "200", description = "OK")
    @ApiResponse(
            responseCode = "409",
            content =
                    @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = CareProblemBody.class)))
    public void submit(
            @PathVariable long id,
            @RequestHeader("Idempotency-Key") String key,
            @RequestBody(required = false) CarePlanSubmit body) {
        service.submit(service.target(id), id, body == null ? null : body.changeReason());
    }

    @PostMapping("/care-plans/{id}/retract")
    @ApiResponse(responseCode = "200", description = "OK")
    @ApiResponse(
            responseCode = "403",
            content =
                    @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = CareProblemBody.class)))
    @ApiResponse(
            responseCode = "409",
            content =
                    @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = CareProblemBody.class)))
    public void retract(@PathVariable long id, @RequestHeader("Idempotency-Key") String key) {
        service.retract(service.target(id), id);
    }

    @PostMapping("/care-plans/{id}/approve")
    @ApiResponse(responseCode = "200", description = "OK")
    @ApiResponse(
            responseCode = "403",
            content =
                    @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = CareProblemBody.class)))
    @ApiResponse(
            responseCode = "409",
            content =
                    @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = CareProblemBody.class)))
    public void approve(
            @PathVariable long id,
            @RequestHeader("Idempotency-Key") String key,
            @RequestBody(required = false) CarePlanApprovalWrite body) {
        service.approve(service.target(id), id, body == null ? null : body.reason());
    }

    @PostMapping("/care-plans/{id}/return")
    @ApiResponse(responseCode = "200", description = "OK")
    @ApiResponse(
            responseCode = "409",
            content =
                    @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = CareProblemBody.class)))
    @ApiResponse(
            responseCode = "422",
            content =
                    @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = CareProblemBody.class)))
    public void returnToDraft(
            @PathVariable long id, @RequestHeader("Idempotency-Key") String key, @RequestBody CareCatalogReason body) {
        service.returnToDraft(service.target(id), id, body.reason());
    }

    @PostMapping("/care-plans/{id}/cancel")
    @ApiResponse(responseCode = "200", description = "OK")
    @ApiResponse(
            responseCode = "409",
            content =
                    @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = CareProblemBody.class)))
    public void cancel(
            @PathVariable long id,
            @RequestHeader("Idempotency-Key") String key,
            @RequestBody CarePlanApprovalWrite body) {
        // Q-449: lý do không bắt buộc khi người lập tự bỏ bản Nháp của mình; service kiểm
        service.cancel(service.target(id), id, body.reason());
    }

    /** Q-448 (FR-059): hàng chờ "Cần lập hoặc sửa kế hoạch" theo phạm vi xem, chờ lâu nhất trước. */
    @GetMapping("/care-plan-reviews/queue")
    @ApiResponse(responseCode = "200", description = "OK")
    public List<CarePlanQueueResponse> reviewQueue() {
        return reviews.queue().stream()
                .map(row -> {
                    var identity = residentApi.identity(row.residentId());
                    return new CarePlanQueueResponse(
                            row.residentId(),
                            identity.fullName(),
                            identity.profileCode(),
                            row.reasons().stream()
                                    .map(CarePlanReviewResponse::of)
                                    .toList(),
                            row.draft().map(CarePlan::id).orElse(null),
                            row.draft()
                                    .map(p -> p.status() == CarePlanStatus.DRAFT && "RETURNED".equals(p.endReason()))
                                    .orElse(false),
                            row.draft()
                                    .map(p -> p.status() == CarePlanStatus.PENDING_APPROVAL)
                                    .orElse(false),
                            row.draft().map(p -> actor(p.createdBy())).orElse(null),
                            row.waitingSince());
                })
                .toList();
    }

    /** Q-448 (FR-059): yêu cầu xem xét Đang mở của một người cao tuổi, hiện phía trên kế hoạch. */
    @GetMapping("/residents/{residentId}/care-plan-reviews")
    @ApiResponse(responseCode = "200", description = "OK")
    @ApiResponse(
            responseCode = "403",
            content =
                    @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = CareProblemBody.class)))
    public List<CarePlanReviewResponse> openReviews(@PathVariable long residentId) {
        return reviews.open(CarePlanService.residentTarget(residentId, 0), residentId).stream()
                .map(CarePlanReviewResponse::of)
                .toList();
    }

    /** Q-448 (FR-058): "Không cần thay đổi" đóng mọi yêu cầu xem xét Đang mở của người cao tuổi. */
    @PostMapping("/residents/{residentId}/care-plan-reviews/dismiss")
    @ApiResponse(responseCode = "200", description = "OK")
    @ApiResponse(
            responseCode = "409",
            content =
                    @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = CareProblemBody.class)))
    public void dismissReviews(
            @PathVariable long residentId,
            @RequestHeader("Idempotency-Key") String key,
            @RequestBody CareCatalogReason body) {
        reviews.dismiss(CarePlanService.residentTarget(residentId, 0), residentId, body.reason());
    }

    private String actor(Long accountId) {
        return accountId == null ? null : accounts.fullName(accountId).orElse(Long.toString(accountId));
    }

    @GetMapping("/care-plans/compare")
    public CarePlanComparisonResponse compare(@RequestParam long from, @RequestParam long to) {
        var before = queries.get(service.target(from), from);
        var after = queries.get(service.target(to), to);
        if (before.residentId() != after.residentId())
            throw new com.eldercare.backend.common.error.InvalidValueException(
                    "Chỉ so sánh hai phiên bản của cùng người cao tuổi");
        return CarePlanComparisonResponse.of(
                com.eldercare.backend.care.internal.careplan.domain.CarePlanDifference.compare(
                        queries.items(from), queries.items(to)));
    }

    @PutMapping("/care-plans/{id}/risk-agreements")
    @ApiResponse(responseCode = "200", description = "OK")
    @ApiResponse(
            responseCode = "409",
            content =
                    @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = CareProblemBody.class)))
    @ApiResponse(
            responseCode = "412",
            content =
                    @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = CareProblemBody.class)))
    @ApiResponse(
            responseCode = "422",
            content =
                    @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = CareProblemBody.class)))
    public void risks(
            @PathVariable long id,
            @RequestHeader("If-Match") String etag,
            @RequestHeader("Idempotency-Key") String key,
            @RequestBody List<CareRiskAgreementWrite> body) {
        requireRows(body);
        var at = time.now();
        risks.replace(
                service.target(id),
                id,
                etag,
                body.stream().map(row -> row.row(id, user.accountId(), at)).toList());
    }

    private void requireRows(List<?> rows) {
        if (rows == null || rows.stream().anyMatch(Objects::isNull))
            throw new com.eldercare.backend.common.error.InvalidValueException("Danh sách không được chứa dòng null");
    }

    private CarePlanResponse response(CarePlan row) {
        var target = CarePlanService.residentTarget(row.residentId(), row.id());
        var commands = new LinkedHashMap<String, PermissionKey>();
        switch (row.status()) {
            case DRAFT -> {
                commands.put("replaceItems", key(Operation.UPDATE));
                commands.put("replaceRiskAgreements", key(Operation.UPDATE));
                commands.put("submit", key(Operation.UPDATE));
                // Q-449: người lập tự bỏ bản Nháp của mình không cần lý do; cùng lệnh cancel, tên khác để giao diện
                // biết
                if (row.createdBy() == user.accountId()) commands.put("discardDraft", key(Operation.VIEW));
                else commands.put("cancel", key(Operation.APPROVE));
            }
            case PENDING_APPROVAL -> {
                commands.put("approve", key(Operation.APPROVE));
                commands.put("return", key(Operation.APPROVE));
                commands.put(
                        "cancel", row.createdBy() == user.accountId() ? key(Operation.VIEW) : key(Operation.APPROVE));
                if (row.createdBy() == user.accountId()) commands.put("retract", key(Operation.UPDATE));
            }
            default -> {}
        }
        return new CarePlanResponse(
                row.id(),
                row.residentId(),
                row.revision(),
                row.status(),
                row.effectiveFrom(),
                row.effectiveTo(),
                actor(row.createdBy()),
                actor(row.approvedBy()),
                row.approvedAt(),
                row.selfApproved(),
                row.changeReason(),
                queries.items(row.id()).stream().map(CarePlanItemResponse::of).toList(),
                queries.risks(row.id()).stream()
                        .map(r -> CareRiskAgreementResponse.of(r, this::actor))
                        .toList(),
                !queries.hasEffectivePlan(row.residentId()),
                permissions.allowedCommands(user, target, commands));
    }

    private PermissionKey key(Operation operation) {
        return new PermissionKey(Function.CARE_PLAN, operation);
    }
}
