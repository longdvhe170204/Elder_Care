package com.eldercare.backend.administration.account.internal.permission.service;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.RequiresPermission;
import com.eldercare.backend.administration.account.api.dto.AccountStatus;
import com.eldercare.backend.administration.account.api.dto.AccountType;
import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.administration.account.internal.auth.domain.AccountAuditActions;
import com.eldercare.backend.administration.account.internal.auth.service.AccountService;
import com.eldercare.backend.administration.account.internal.domain.Account;
import com.eldercare.backend.administration.account.internal.permission.domain.PermissionViews.AccountPermissions;
import com.eldercare.backend.administration.account.internal.persistence.AccountStore;
import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.administration.auditlog.api.dto.AuditAction;
import com.eldercare.backend.administration.auditlog.api.dto.AuditLogEntry;
import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.data.BusinessCommand;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.error.ConditionNotMetException;
import com.eldercare.backend.common.error.InvalidValueException;
import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.common.error.UnmetCondition;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.common.web.VersionEtag;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gán, gỡ vai trò tài khoản nhân viên (002 T033; FR-003, FR-004, FR-012, FR-026, FR-027; BR-M15-04, BR-M15-17): chỉ
 * Quản lý viện, có lý do, {@code If-Match} là {@code permission_version}; đóng dòng cũ hoặc thêm dòng mới (không xóa,
 * research A-05), tăng {@code permission_version} nên yêu cầu kế tiếp tính lại quyền; không gỡ vai trò cuối cùng; không
 * làm viện mất Quản lý viện Hoạt động cuối cùng (đếm dưới {@code SELECT … FOR UPDATE}).
 */
@Service
public class RoleAssignmentService {

    private final AccountStore store;
    private final AccountPermissionsQuery permissions;
    private final ObjectProvider<CurrentUser> currentUser;
    private final AuditLogApi auditLog;
    private final BusinessTime time;

    RoleAssignmentService(
            AccountStore store,
            AccountPermissionsQuery permissions,
            ObjectProvider<CurrentUser> currentUser,
            AuditLogApi auditLog,
            BusinessTime time) {
        this.store = store;
        this.permissions = permissions;
        this.currentUser = currentUser;
        this.auditLog = auditLog;
        this.time = time;
    }

    /** Tập quyền kèm {@code permission_version} để đặt ETag. */
    public record Result(AccountPermissions permissions, int permissionVersion) {}

    /**
     * Tập quyền hiện hành kèm {@code permission_version} cho {@code GET /accounts/{id}/permissions} (FR-026a; chỉ Quản
     * lý viện); màn T035 cần ETag này cho lệnh gán, gỡ vai trò.
     */
    @RequiresPermission(function = Function.ACCOUNT_PERMISSION_PARAMETER, operation = Operation.VIEW)
    @Transactional(readOnly = true)
    public Result current(long accountId) {
        AccountService.requireManager(currentUser.getObject());
        Account account = store.find(accountId).orElseThrow(NotFoundException::new);
        return new Result(view(account), account.permissionVersion());
    }

    /**
     * Tập quyền tại thời điểm {@code asOf} (002 T076; FR-026a, BR-M15-16): vai trò, trạng thái, thu hẹp, quyền gán thêm
     * có hiệu lực lúc đó; ETag vẫn là {@code permission_version} hiện hành. Thời điểm tương lai thì 422 vì quyền không
     * đặt trước được (FR-027).
     */
    @RequiresPermission(function = Function.ACCOUNT_PERMISSION_PARAMETER, operation = Operation.VIEW)
    @Transactional(readOnly = true)
    public Result at(long accountId, Instant asOf) {
        AccountService.requireManager(currentUser.getObject());
        Account account = store.find(accountId).orElseThrow(NotFoundException::new);
        if (asOf.isAfter(time.now())) {
            throw new InvalidValueException("Không tra quyền tại thời điểm tương lai (FR-027)");
        }
        AccountPermissions permissionsAt = permissions.at(
                accountId,
                asOf,
                store.statusAt(accountId, asOf, account.status()).name(),
                codes(store.rolesAt(accountId, asOf)));
        return new Result(permissionsAt, account.permissionVersion());
    }

    @BusinessCommand
    @RequiresPermission(function = Function.ACCOUNT_PERMISSION_PARAMETER, operation = Operation.UPDATE)
    @Transactional
    public Result assign(long accountId, String ifMatch, Role role, Reason reason) {
        CurrentUser user = currentUser.getObject();
        Account account = locked(accountId, ifMatch, user);
        Set<Role> before = store.roles(accountId);
        if (before.contains(role)) {
            throw new ConditionNotMetException(List.of(new UnmetCondition("FR-003", "Tài khoản đã có vai trò này")));
        }
        store.addRole(accountId, role, user.accountId(), reason.value());
        return changed(account, before, AccountAuditActions.ROLE_ASSIGNED, reason);
    }

    @BusinessCommand
    @RequiresPermission(function = Function.ACCOUNT_PERMISSION_PARAMETER, operation = Operation.UPDATE)
    @Transactional
    public Result remove(long accountId, String ifMatch, Role role, Reason reason) {
        CurrentUser user = currentUser.getObject();
        Account account = locked(accountId, ifMatch, user);
        Set<Role> before = store.roles(accountId);
        if (!before.contains(role)) {
            throw new ConditionNotMetException(List.of(new UnmetCondition("FR-003", "Tài khoản không có vai trò này")));
        }
        if (before.size() == 1) {
            throw new ConditionNotMetException(
                    List.of(new UnmetCondition("FR-004", "Tài khoản nhân viên phải còn ít nhất một vai trò")));
        }
        if (role == Role.NURSING_HOME_MANAGER
                && account.status() == AccountStatus.ACTIVE
                && store.otherActiveManagers(accountId) == 0) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "NO_MANAGER_LEFT",
                    "Viện phải còn ít nhất một Quản lý viện Hoạt động (FR-012)");
        }
        store.closeRole(accountId, role);
        return changed(account, before, AccountAuditActions.ROLE_REMOVED, reason);
    }

    private Account locked(long accountId, String ifMatch, CurrentUser user) {
        AccountService.requireManager(user);
        Account account = store.lock(accountId).orElseThrow(NotFoundException::new);
        if (account.type() != AccountType.STAFF) {
            throw new ConditionNotMetException(
                    List.of(new UnmetCondition("FR-003", "Chỉ gán, gỡ vai trò nhân viên cho tài khoản nhân viên")));
        }
        VersionEtag.requireMatch(ifMatch, account.permissionVersion(), view(account));
        return account;
    }

    private Result changed(Account account, Set<Role> before, AuditAction action, Reason reason) {
        store.bumpPermissionVersion(account.id());
        auditLog.write(AuditLogEntry.builder()
                .action(action)
                .entity(AccountAuditActions.ENTITY, account.id())
                .before(Map.of("roles", codes(before)))
                .after(Map.of("roles", codes(store.roles(account.id()))))
                .reason(reason)
                .build());
        Account updated = store.find(account.id()).orElseThrow();
        return new Result(view(updated), updated.permissionVersion());
    }

    private AccountPermissions view(Account account) {
        return permissions.of(account.id(), account.status().name(), codes(store.roles(account.id())));
    }

    private static List<String> codes(Set<Role> roles) {
        return roles.stream().map(Role::code).sorted().toList();
    }
}
