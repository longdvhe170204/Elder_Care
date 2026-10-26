package com.cambae.backend.m02.internal.ending.service;

import com.cambae.backend.common.Role;
import com.cambae.backend.common.error.InvalidValueException;
import com.cambae.backend.common.error.NotFoundException;
import com.cambae.backend.common.error.PermissionDeniedException;
import com.cambae.backend.common.error.UnmetCondition;
import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.m02.api.events.EndingExceptionApproved;
import com.cambae.backend.m02.internal.ending.domain.EndingConditionCode;
import com.cambae.backend.m02.internal.ending.domain.ResidencyEnding;
import com.cambae.backend.m02.internal.ending.persistence.ResidencyEndingStore;
import com.cambae.backend.m02.internal.transition.service.ResidencyLock;
import com.cambae.backend.m15.account.api.CurrentUser;
import com.cambae.backend.m15.account.api.dto.Function;
import com.cambae.backend.m15.account.api.dto.Operation;
import com.cambae.backend.m15.account.api.dto.PermissionKey;
import com.cambae.backend.m15.account.api.dto.Target;
import com.cambae.backend.m15.approval.api.ApprovalRequestView;
import com.cambae.backend.m15.approval.api.ResultRef;
import com.cambae.backend.m15.approval.api.spi.RequestTypeHandler;
import com.cambae.backend.m15.auditlog.api.AuditAction;
import com.cambae.backend.m15.auditlog.api.AuditLogApi;
import com.cambae.backend.m15.auditlog.api.AuditLogEntry;
import com.cambae.backend.m15.auditlog.api.Basis;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

/**
 * Loại yêu cầu {@code M02.ENDING_EXCEPTION} (FR-064, Q-379, Q-313; research T-09): ngoại lệ cho một điều kiện (a) → (d),
 * (h) của hồ sơ kết thúc. Nội dung {@code {residencyEndingId, conditionCode}}. Hành chính lập; Kế toán chỉ với ý tiền của (h)
 * ({@code END_H_SETTLEMENT}, Phụ lục 27 chú thích 39); Quản lý viện duyệt. {@code END_E_HANDOVER} và {@code
 * END_H_SETTLEMENT_CONFIRMATION} không có ngoại lệ (422). Có ngày hiệu lực là ngày duyệt nên áp dụng ngay: thêm dòng ngoại lệ
 * và báo 017 tính lại điều kiện quyết toán. Hồ sơ không còn Đang chuẩn bị thì không duyệt, không áp dụng được.
 */
@Component
class EndingExceptionHandler implements RequestTypeHandler {

    static final String TYPE = "M02.ENDING_EXCEPTION";
    /** Đối tượng của yêu cầu ngoại lệ ý tiền (h); chú thích 39 cho phép Kế toán khi đối tượng này. */
    static final String SETTLEMENT_ENTITY = "M02.ENDING_EXCEPTION.SETTLEMENT";

    static final String EXCEPTION_ENTITY = "M02.ENDING_EXCEPTION";
    static final AuditAction APPROVED = new AuditAction("M02_ENDING_EXCEPTION_APPROVED", false);

    private final ResidencyEndingStore store;
    private final ResidencyLock residencyLock;
    private final AuditLogApi auditLog;
    private final ApplicationEventPublisher events;
    private final BusinessTime time;
    private final ObjectProvider<CurrentUser> currentUser;

    EndingExceptionHandler(
            ResidencyEndingStore store,
            ResidencyLock residencyLock,
            AuditLogApi auditLog,
            ApplicationEventPublisher events,
            BusinessTime time,
            ObjectProvider<CurrentUser> currentUser) {
        this.store = store;
        this.residencyLock = residencyLock;
        this.auditLog = auditLog;
        this.events = events;
        this.time = time;
        this.currentUser = currentUser;
    }

    @Override
    public String typeCode() {
        return TYPE;
    }

    @Override
    public void validateContent(JsonNode content) {
        endingId(content);
        EndingConditionCode code = code(content);
        CurrentUser user = currentUser.getObject();
        boolean office = user.roles().contains(Role.OFFICE_STAFF);
        boolean accountantForMoney =
                user.roles().contains(Role.ACCOUNTANT) && code == EndingConditionCode.END_H_SETTLEMENT;
        if (!office && !accountantForMoney) {
            throw new PermissionDeniedException(
                    "Hành chính lập ngoại lệ điều kiện kết thúc; Kế toán chỉ với ý tiền của điều kiện (h) (FR-064)");
        }
    }

    /** Hành chính, Kế toán (chú thích 39) lập (Phụ lục 27 dòng "Kết thúc lưu trú, qua đời", ô T). */
    @Override
    public PermissionKey createPermission() {
        return new PermissionKey(Function.RESIDENCY_END_DEATH, Operation.CREATE);
    }

    /** Quản lý viện duyệt (ô D). */
    @Override
    public PermissionKey approvePermission() {
        return new PermissionKey(Function.RESIDENCY_END_DEATH, Operation.APPROVE);
    }

    @Override
    public Target scope(JsonNode content) {
        long id = endingId(content);
        String entity = code(content) == EndingConditionCode.END_H_SETTLEMENT ? SETTLEMENT_ENTITY : EXCEPTION_ENTITY;
        return store.find(id)
                .map(ending -> new Target(entity, String.valueOf(ending.id()), ending.residentId(), null))
                .orElse(new Target(entity, String.valueOf(id), null, null));
    }

    @Override
    public List<UnmetCondition> checkOnApprove(ApprovalRequestView request) {
        return notPreparing(request);
    }

    @Override
    public List<UnmetCondition> checkApplyConditions(ApprovalRequestView request) {
        return notPreparing(request);
    }

    /** DBR: mỗi yêu cầu cho đúng một dòng ngoại lệ; thêm dòng, nhật ký và báo 017 trong transaction áp dụng. */
    @Override
    public ResultRef apply(ApprovalRequestView request, LocalDate actualEffectiveDate) {
        long endingId = endingId(request.content());
        ResidencyEnding found = store.find(endingId).orElseThrow(NotFoundException::new);
        residencyLock.lock(found.residentId());
        ResidencyEnding ending = store.lock(endingId).orElseThrow(NotFoundException::new);
        EndingConditionCode code = code(request.content());
        long id = store.insertException(endingId, ending.residentId(), code, request.id(), time.now());
        auditLog.write(AuditLogEntry.builder()
                .action(APPROVED)
                .entity(ResidencyEndingService.ENTITY_TYPE, endingId)
                .resident(ending.residentId())
                .after(Map.of("conditionCode", code.name(), "exceptionId", id))
                .basis(new Basis(null, request.id(), null, null))
                .build());
        events.publishEvent(new EndingExceptionApproved(endingId, ending.residentId(), code.name()));
        return new ResultRef("M02.ENDING_CONDITION_EXCEPTION", String.valueOf(id));
    }

    private List<UnmetCondition> notPreparing(ApprovalRequestView request) {
        return store.find(endingId(request.content()))
                        .filter(ResidencyEnding::preparing)
                        .isPresent()
                ? List.of()
                : List.of(new UnmetCondition(
                        "RESIDENCY_ENDING_NOT_PREPARING",
                        "Hồ sơ kết thúc lưu trú không còn Đang chuẩn bị nên không áp dụng ngoại lệ (FR-064, FR-067)"));
    }

    private static long endingId(JsonNode content) {
        JsonNode id = content == null ? null : content.get("residencyEndingId");
        if (id == null || !id.isIntegralNumber() || id.asLong() <= 0) {
            throw new InvalidValueException("Nội dung cần residencyEndingId là mã số nguyên dương");
        }
        return id.asLong();
    }

    private static EndingConditionCode code(JsonNode content) {
        JsonNode code = content == null ? null : content.get("conditionCode");
        if (code == null || !code.isString()) {
            throw new InvalidValueException("Nội dung cần conditionCode");
        }
        return ResidencyEndingService.exceptionCode(code.asString());
    }
}
