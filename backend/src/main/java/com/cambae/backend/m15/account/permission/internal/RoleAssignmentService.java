package com.cambae.backend.m15.account.permission.internal;

import com.cambae.backend.common.Reason;
import com.cambae.backend.common.Role;
import com.cambae.backend.common.data.BusinessCommand;
import com.cambae.backend.common.error.BusinessException;
import com.cambae.backend.common.error.ConditionNotMetException;
import com.cambae.backend.common.error.NotFoundException;
import com.cambae.backend.common.error.UnmetCondition;
import com.cambae.backend.common.web.VersionEtag;
import com.cambae.backend.m15.account.api.AccountStatus;
import com.cambae.backend.m15.account.api.AccountType;
import com.cambae.backend.m15.account.api.CurrentUser;
import com.cambae.backend.m15.account.api.Function;
import com.cambae.backend.m15.account.api.Operation;
import com.cambae.backend.m15.account.api.RequiresPermission;
import com.cambae.backend.m15.account.auth.internal.AccountAuditActions;
import com.cambae.backend.m15.account.auth.internal.AccountService;
import com.cambae.backend.m15.account.auth.internal.AccountStore;
import com.cambae.backend.m15.account.auth.internal.AccountStore.AccountRow;
import com.cambae.backend.m15.account.permission.internal.PermissionViews.AccountPermissions;
import com.cambae.backend.m15.auditlog.api.AuditAction;
import com.cambae.backend.m15.auditlog.api.AuditLogApi;
import com.cambae.backend.m15.auditlog.api.AuditLogEntry;
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

    RoleAssignmentService(
            AccountStore store,
            AccountPermissionsQuery permissions,
            ObjectProvider<CurrentUser> currentUser,
            AuditLogApi auditLog) {
        this.store = store;
        this.permissions = permissions;
        this.currentUser = currentUser;
        this.auditLog = auditLog;
    }

    /** Tập quyền kèm {@code permission_version} để đặt ETag. */
    public record Result(AccountPermissions permissions, int permissionVersion) {}

    /**
     * Tập quyền hiện hành kèm {@code permission_version} cho {@code GET /accounts/{id}/permissions} (FR-026a; chỉ Quản
     * lý viện). Phần {@code asOf} làm ở T076; màn T035 cần ETag này cho lệnh gán, gỡ vai trò.
     */
    @RequiresPermission(function = Function.ACCOUNT_PERMISSION_PARAMETER, operation = Operation.VIEW)
    @Transactional(readOnly = true)
    public Result current(long accountId) {
        AccountService.requireManager(currentUser.getObject());
        AccountRow account = store.find(accountId).orElseThrow(NotFoundException::new);
        return new Result(view(account), account.permissionVersion());
    }

    @BusinessCommand
    @RequiresPermission(function = Function.ACCOUNT_PERMISSION_PARAMETER, operation = Operation.UPDATE)
    @Transactional
    public Result assign(long accountId, String ifMatch, Role role, Reason reason) {
        CurrentUser user = currentUser.getObject();
        AccountRow account = locked(accountId, ifMatch, user);
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
        AccountRow account = locked(accountId, ifMatch, user);
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

    private AccountRow locked(long accountId, String ifMatch, CurrentUser user) {
        AccountService.requireManager(user);
        AccountRow account = store.lock(accountId).orElseThrow(NotFoundException::new);
        if (account.type() != AccountType.STAFF) {
            throw new ConditionNotMetException(
                    List.of(new UnmetCondition("FR-003", "Chỉ gán, gỡ vai trò nhân viên cho tài khoản nhân viên")));
        }
        VersionEtag.requireMatch(ifMatch, account.permissionVersion(), view(account));
        return account;
    }

    private Result changed(AccountRow account, Set<Role> before, AuditAction action, Reason reason) {
        store.bumpPermissionVersion(account.id());
        auditLog.write(AuditLogEntry.builder()
                .action(action)
                .entity(AccountAuditActions.ENTITY, account.id())
                .before(Map.of("roles", codes(before)))
                .after(Map.of("roles", codes(store.roles(account.id()))))
                .reason(reason)
                .build());
        AccountRow updated = store.find(account.id()).orElseThrow();
        return new Result(view(updated), updated.permissionVersion());
    }

    private AccountPermissions view(AccountRow account) {
        return permissions.of(account.id(), account.status().name(), codes(store.roles(account.id())));
    }

    private static List<String> codes(Set<Role> roles) {
        return roles.stream().map(Role::code).sorted().toList();
    }
}
