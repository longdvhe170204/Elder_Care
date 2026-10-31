package com.eldercare.backend.care.web;

import com.eldercare.backend.administration.account.api.*;
import com.eldercare.backend.administration.account.api.dto.*;
import com.eldercare.backend.care.internal.careplan.domain.CarePlan;
import com.eldercare.backend.care.internal.careplan.service.*;
import com.eldercare.backend.care.web.request.*;
import com.eldercare.backend.care.web.response.*;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.common.web.VersionEtag;
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

    public CarePlanController(
            CarePlanService service,
            CarePlanQueries queries,
            AccountApi accounts,
            PermissionApi permissions,
            CurrentUser user,
            CareRiskService risks,
            BusinessTime time) {
        this.service = service;
        this.queries = queries;
        this.accounts = accounts;
        this.permissions = permissions;
        this.user = user;
        this.risks = risks;
        this.time = time;
    }

    @PostMapping("/residents/{residentId}/care-plans")
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
        var row = service.create(
                CarePlanService.residentTarget(residentId, 0), residentId, !Boolean.FALSE.equals(body.copyCurrent()));
        return ResponseEntity.status(201).eTag(VersionEtag.of(row.version())).body(response(row));
    }

    @GetMapping("/residents/{residentId}/care-plans")
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
    public void submit(
            @PathVariable long id, @RequestHeader("Idempotency-Key") String key, @RequestBody CarePlanSubmit body) {
        service.submit(service.target(id), id, body.effectiveFrom(), body.changeReason());
    }

    @PostMapping("/care-plans/{id}/approve")
    public void approve(
            @PathVariable long id,
            @RequestHeader("Idempotency-Key") String key,
            @RequestBody(required = false) CareCatalogReason body) {
        service.approve(service.target(id), id, body == null ? null : body.reason());
    }

    @PostMapping("/care-plans/{id}/return")
    public void returnToDraft(
            @PathVariable long id, @RequestHeader("Idempotency-Key") String key, @RequestBody CareCatalogReason body) {
        service.returnToDraft(service.target(id), id, body.reason());
    }

    @PostMapping("/care-plans/{id}/cancel")
    public void cancel(
            @PathVariable long id, @RequestHeader("Idempotency-Key") String key, @RequestBody CareCatalogReason body) {
        service.cancel(service.target(id), id, body.reason());
    }

    @PostMapping("/care-plans/{id}/withdraw-approval")
    public void withdraw(
            @PathVariable long id, @RequestHeader("Idempotency-Key") String key, @RequestBody CareCatalogReason body) {
        service.withdraw(service.target(id), id, body.reason());
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
                commands.put(
                        "cancel", row.createdBy() == user.accountId() ? key(Operation.VIEW) : key(Operation.APPROVE));
            }
            case PENDING_APPROVAL -> {
                commands.put("approve", key(Operation.APPROVE));
                commands.put("return", key(Operation.APPROVE));
                commands.put(
                        "cancel", row.createdBy() == user.accountId() ? key(Operation.VIEW) : key(Operation.APPROVE));
            }
            case PENDING_EFFECT -> commands.put("withdrawApproval", key(Operation.APPROVE));
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
