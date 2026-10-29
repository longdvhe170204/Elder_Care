package com.eldercare.backend.administration.account.internal.auth.service;

import com.eldercare.backend.administration.account.api.dto.AccountStatus;
import com.eldercare.backend.administration.account.api.dto.AccountType;
import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.administration.account.api.dto.PermissionKey;
import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.administration.account.internal.auth.domain.AccountAuditActions;
import com.eldercare.backend.administration.account.internal.persistence.AccountStore;
import com.eldercare.backend.administration.approval.api.ApprovalRequestView;
import com.eldercare.backend.administration.approval.api.ResultRef;
import com.eldercare.backend.administration.approval.api.spi.RequestTypeHandler;
import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.error.InvalidValueException;
import com.eldercare.backend.common.error.UnmetCondition;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Loại yêu cầu {@code M15.RESIDENT_ACCOUNT_LOCK} (002 T094; FR-072, BR-M15-23; research A-16): người thân đề nghị khóa
 * tài khoản người cao tuổi thì Hành chính lập yêu cầu; chỉ Quản lý viện duyệt, không tự duyệt; áp dụng thì khóa với
 * nguyên nhân {@code FAMILY_REQUEST}. Nội dung {@code {accountId, requestingFamilyMemberId, reason}}.
 */
@Component
class ResidentAccountLockRequestHandler implements RequestTypeHandler {

    static final String TYPE = "M15.RESIDENT_ACCOUNT_LOCK";

    /** Nội dung yêu cầu (A-16 {@code {taiKhoanId, nguoiThanDeNghiId, lyDo}}). */
    record Content(Long accountId, Long requestingFamilyMemberId, String reason) {}

    private final AccountStore store;
    private final ResidentAccountService residents;
    private final JsonMapper json;

    ResidentAccountLockRequestHandler(AccountStore store, ResidentAccountService residents, JsonMapper json) {
        this.store = store;
        this.residents = residents;
        this.json = json;
    }

    @Override
    public String typeCode() {
        return TYPE;
    }

    @Override
    public void validateContent(JsonNode content) {
        Content c = read(content);
        if (store.find(c.accountId())
                .filter(a -> a.type() == AccountType.RESIDENT)
                .isEmpty()) {
            throw new InvalidValueException("accountId phải là tài khoản người cao tuổi");
        }
    }

    /** Hành chính lập (Phụ lục 27 dòng "Tài khoản người cao tuổi", ô T). */
    @Override
    public PermissionKey createPermission() {
        return new PermissionKey(Function.RESIDENT_ACCOUNT, Operation.CREATE);
    }

    /** Dòng không có ô D; người duyệt giới hạn ở Quản lý viện qua {@link #approverRoleRestriction}. */
    @Override
    public PermissionKey approvePermission() {
        return new PermissionKey(Function.RESIDENT_ACCOUNT, Operation.UPDATE);
    }

    @Override
    public Target scope(JsonNode content) {
        Content c = read(content);
        return store.find(c.accountId())
                .map(ResidentAccountService::target)
                .orElse(new Target(AccountAuditActions.ENTITY, String.valueOf(c.accountId()), null, null));
    }

    @Override
    public Optional<Set<Role>> approverRoleRestriction(ApprovalRequestView request) {
        return Optional.of(Set.of(Role.NURSING_HOME_MANAGER));
    }

    @Override
    public boolean allowSelfApproval() {
        return false;
    }

    @Override
    public List<UnmetCondition> checkApplyConditions(ApprovalRequestView request) {
        Content c = read(request.content());
        boolean lockable = store.find(c.accountId())
                .map(a -> a.status() == AccountStatus.ACTIVE || a.status() == AccountStatus.TEMPORARILY_LOCKED)
                .orElse(false);
        return lockable
                ? List.of()
                : List.of(new UnmetCondition("FR-072", "Tài khoản không còn Hoạt động hoặc Khóa tạm"));
    }

    @Override
    public ResultRef apply(ApprovalRequestView request, LocalDate actualEffectiveDate) {
        Content c = read(request.content());
        residents.lockOnFamilyRequest(c.accountId(), request.id(), new Reason(request.reason()));
        return new ResultRef(AccountAuditActions.ENTITY, String.valueOf(c.accountId()));
    }

    private Content read(JsonNode content) {
        try {
            Content c = json.treeToValue(content, Content.class);
            if (c == null
                    || c.accountId() == null
                    || c.requestingFamilyMemberId() == null
                    || c.reason() == null
                    || c.reason().isBlank()) {
                throw new InvalidValueException("Nội dung cần accountId, requestingFamilyMemberId, reason");
            }
            return c;
        } catch (JacksonException e) {
            throw new InvalidValueException("Nội dung yêu cầu khóa tài khoản người cao tuổi không hợp lệ");
        }
    }
}
