package com.cambae.backend.m02.internal.contract.service;

import com.cambae.backend.common.error.InvalidValueException;
import com.cambae.backend.common.error.UnmetCondition;
import com.cambae.backend.m02.api.dto.ContractStatus;
import com.cambae.backend.m02.internal.contract.domain.Contract;
import com.cambae.backend.m02.internal.contract.domain.NonStandardTerm;
import com.cambae.backend.m02.internal.contract.persistence.ContractStore;
import com.cambae.backend.m02.internal.transition.service.ResidencyLock;
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
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

/**
 * Loại yêu cầu {@code M02.CONTRACT_TERMS_APPROVAL} (FR-025a; research T-04): Hành chính gửi các điều khoản khác chuẩn của
 * hợp đồng Nháp, Quản lý viện duyệt. Nội dung {@code {contractId, contentHash, terms}}; áp dụng khi yêu cầu được duyệt thì mở
 * khóa nội dung và giữ yêu cầu cùng bản băm làm căn cứ cho Gửi ký. Từ chối hoặc hủy yêu cầu thì mở khóa ở {@link
 * ContractTermsListeners}.
 */
@Component
class ContractTermsRequestHandler implements RequestTypeHandler {

    static final String TYPE = "M02.CONTRACT_TERMS_APPROVAL";
    static final AuditAction APPROVED = new AuditAction("M02_CONTRACT_TERMS_APPROVED", false);

    private static final Pattern SHA_256 = Pattern.compile("[0-9a-f]{64}");

    private final ContractStore store;
    private final ResidencyLock residencyLock;
    private final AuditLogApi auditLog;

    ContractTermsRequestHandler(ContractStore store, ResidencyLock residencyLock, AuditLogApi auditLog) {
        this.store = store;
        this.residencyLock = residencyLock;
        this.auditLog = auditLog;
    }

    @Override
    public String typeCode() {
        return TYPE;
    }

    @Override
    public void validateContent(JsonNode content) {
        contractId(content);
        JsonNode hash = content.get("contentHash");
        if (hash == null
                || !hash.isString()
                || !SHA_256.matcher(hash.asString()).matches()) {
            throw new InvalidValueException("Nội dung cần contentHash là SHA-256 thập lục phân");
        }
        JsonNode terms = content.get("terms");
        if (terms == null || !terms.isArray() || terms.isEmpty()) {
            throw new InvalidValueException("Nội dung cần ít nhất một điều khoản khác chuẩn trong terms");
        }
        for (JsonNode term : terms) {
            JsonNode type = term.get("type");
            try {
                NonStandardTerm.Type.valueOf(type == null ? "" : type.asString());
            } catch (IllegalArgumentException e) {
                throw new InvalidValueException("terms[].type phải là PRICE, ABSENCE_POLICY hoặc BED_HOLD_POLICY");
            }
        }
    }

    /** Hành chính lập (Phụ lục 27 dòng "Tiếp nhận, hợp đồng, đặt cọc", ô T). */
    @Override
    public PermissionKey createPermission() {
        return new PermissionKey(Function.ADMISSION_CONTRACT_DEPOSIT, Operation.UPDATE);
    }

    /** Quản lý viện duyệt (ô D, chú thích 6). */
    @Override
    public PermissionKey approvePermission() {
        return new PermissionKey(Function.ADMISSION_CONTRACT_DEPOSIT, Operation.APPROVE);
    }

    @Override
    public Target scope(JsonNode content) {
        long id = contractId(content);
        return store.find(id)
                .map(contract -> ContractService.target(contract.id(), contract.residentId()))
                .orElse(new Target(ContractService.ENTITY_TYPE, String.valueOf(id), null, null));
    }

    @Override
    public List<UnmetCondition> checkApplyConditions(ApprovalRequestView request) {
        Contract contract = store.find(contractId(request.content())).orElse(null);
        boolean valid = contract != null
                && contract.status() == ContractStatus.DRAFT
                && contract.awaitingTermsApproval()
                && Long.valueOf(request.id()).equals(contract.termsApprovalRequestId())
                && request.content().get("contentHash").asString().equals(contract.contentHash());
        return valid
                ? List.of()
                : List.of(new UnmetCondition(
                        "FR-025a", "Hợp đồng không còn ở Nháp chờ duyệt đúng nội dung đã gửi, không áp dụng được"));
    }

    @Override
    public ResultRef apply(ApprovalRequestView request, LocalDate actualEffectiveDate) {
        long contractId = contractId(request.content());
        Contract found = store.find(contractId).orElseThrow();
        residencyLock.lock(found.residentId());
        store.lock(contractId).orElseThrow();
        store.termsApproved(contractId);
        auditLog.write(AuditLogEntry.builder()
                .action(APPROVED)
                .entity(ContractService.ENTITY_TYPE, contractId)
                .resident(found.residentId())
                .before(Map.of("awaitingTermsApproval", true))
                .after(Map.of("awaitingTermsApproval", false))
                .basis(new Basis(null, request.id(), contractId, null))
                .build());
        return new ResultRef(ContractService.ENTITY_TYPE, String.valueOf(contractId));
    }

    static long contractId(JsonNode content) {
        JsonNode node = content == null ? null : content.get("contractId");
        if (node == null || !node.isIntegralNumber() || node.asLong() <= 0) {
            throw new InvalidValueException("Nội dung cần contractId là mã hợp đồng");
        }
        return node.asLong();
    }
}
