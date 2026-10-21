package com.cambae.backend.m15.account.internal.permission.service;

import com.cambae.backend.common.Reason;
import com.cambae.backend.common.Role;
import com.cambae.backend.common.data.BusinessCommand;
import com.cambae.backend.common.error.BusinessException;
import com.cambae.backend.common.error.ConditionNotMetException;
import com.cambae.backend.common.error.InvalidValueException;
import com.cambae.backend.common.error.NotFoundException;
import com.cambae.backend.common.error.UnmetCondition;
import com.cambae.backend.common.error.VersionMismatchException;
import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.common.web.VersionEtag;
import com.cambae.backend.m15.account.api.CurrentUser;
import com.cambae.backend.m15.account.api.RequiresPermission;
import com.cambae.backend.m15.account.api.dto.Function;
import com.cambae.backend.m15.account.api.dto.Operation;
import com.cambae.backend.m15.account.api.dto.PermissionKey;
import com.cambae.backend.m15.account.internal.auth.domain.AccountAuditActions;
import com.cambae.backend.m15.account.internal.auth.service.AccountService;
import com.cambae.backend.m15.account.internal.domain.Account;
import com.cambae.backend.m15.account.internal.permission.domain.PermissionAuditActions;
import com.cambae.backend.m15.account.internal.permission.domain.PermissionScope;
import com.cambae.backend.m15.account.internal.permission.domain.PermissionViews.AccountPermissions;
import com.cambae.backend.m15.account.internal.permission.domain.PermissionViews.EffectivePermission;
import com.cambae.backend.m15.account.internal.permission.domain.PermissionViews.RestrictionKind;
import com.cambae.backend.m15.account.internal.permission.domain.PermissionViews.RoleInfo;
import com.cambae.backend.m15.account.internal.permission.domain.PermissionViews.RolePermissions;
import com.cambae.backend.m15.account.internal.permission.persistence.PermissionChanges;
import com.cambae.backend.m15.account.internal.permission.persistence.PermissionChanges.RestrictionRow;
import com.cambae.backend.m15.account.internal.persistence.AccountStore;
import com.cambae.backend.m15.auditlog.api.AuditAction;
import com.cambae.backend.m15.auditlog.api.AuditLogApi;
import com.cambae.backend.m15.auditlog.api.AuditLogEntry;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Quản lý viện điều chỉnh quyền trong giới hạn ma trận (002 T075; FR-024 → FR-027, FR-012; BR-M15-04, BR-M15-16 →
 * 18; research A-05):
 *
 * <ul>
 *   <li>thu hẹp theo vai trò hoặc tài khoản: khóa phải có trong ô ma trận của vai trò (hoặc của một vai trò đang gán của
 *       tài khoản), hạ phạm vi chỉ khi ô là toàn viện; không thì 409 {@code EXCEEDS_MAXIMUM_PERMISSION};
 *   <li>khôi phục: đóng một dòng thu hẹp đang hiệu lực, quyền về đúng mức ma trận;
 *   <li>gán, gỡ quyền Duyệt kế hoạch chăm sóc cho tài khoản đang có vai trò Điều dưỡng (ngoại lệ duy nhất, Q-07).
 * </ul>
 *
 * <p>Mọi lệnh có lý do, {@code If-Match} là {@code permission_version} của vai trò hoặc tài khoản (sai thì 412 kèm
 * trạng thái hiện hành), tăng số phiên bản nên yêu cầu kế tiếp tính lại quyền (FR-027), ghi nhật ký tập quyền trước,
 * sau dạng {@code CHỨC_NĂNG/THAO_TÁC/PHẠM_VI}. Thu hẹp {@code ACCOUNT_PERMISSION_PARAMETER} mà không còn tài khoản Quản
 * lý viện Hoạt động nào giữ thao tác đó toàn viện thì 409 {@code NO_MANAGER_LEFT}.
 */
@Service
public class PermissionChangeService {

    private final PermissionChanges changes;
    private final AccountStore accounts;
    private final AccountPermissionsQuery permissions;
    private final RolePermissionCalculator calculator;
    private final ObjectProvider<CurrentUser> currentUser;
    private final AuditLogApi auditLog;
    private final BusinessTime time;

    PermissionChangeService(
            PermissionChanges changes,
            AccountStore accounts,
            AccountPermissionsQuery permissions,
            RolePermissionCalculator calculator,
            ObjectProvider<CurrentUser> currentUser,
            AuditLogApi auditLog,
            BusinessTime time) {
        this.changes = changes;
        this.accounts = accounts;
        this.permissions = permissions;
        this.calculator = calculator;
        this.currentUser = currentUser;
        this.auditLog = auditLog;
        this.time = time;
    }

    /** Nội dung lệnh thu hẹp (contracts {@code RestrictBody}). */
    public record RestrictionInput(
            String function, Operation operation, RestrictionKind kind, String metricGroup, String channel) {}

    /** Kết quả theo vai trò kèm {@code role.permission_version} để đặt ETag. */
    public record RoleResult(RolePermissions permissions, int permissionVersion) {}

    @RequiresPermission(function = Function.ACCOUNT_PERMISSION_PARAMETER, operation = Operation.VIEW)
    @Transactional(readOnly = true)
    public List<RoleInfo> roles() {
        AccountService.requireManager(currentUser.getObject());
        return changes.roles();
    }

    @RequiresPermission(function = Function.ACCOUNT_PERMISSION_PARAMETER, operation = Operation.VIEW)
    @Transactional(readOnly = true)
    public RoleResult role(String role) {
        AccountService.requireManager(currentUser.getObject());
        int version = changes.roleVersion(role).orElseThrow(NotFoundException::new);
        return new RoleResult(roleView(role), version);
    }

    @BusinessCommand
    @RequiresPermission(function = Function.ACCOUNT_PERMISSION_PARAMETER, operation = Operation.UPDATE)
    @Transactional
    public RoleResult restrictRole(String role, String ifMatch, RestrictionInput input, Reason reason) {
        CurrentUser user = currentUser.getObject();
        RolePermissions before = lockedRole(role, ifMatch, user);
        PermissionKey key = key(input);
        requireWithinMatrix(Set.of(role), key, input.kind());
        requireNotRestricted(changes.activeRestrictions(role, null), key, input);
        long id = changes.insertRestriction(
                role, null, key, input.kind(), input.metricGroup(), input.channel(), user.accountId(), reason.value());
        requireManagerLeft(key);
        return roleChanged(role, before, PermissionAuditActions.PERMISSION_RESTRICTED, reason, details(id, input));
    }

    @BusinessCommand
    @RequiresPermission(function = Function.ACCOUNT_PERMISSION_PARAMETER, operation = Operation.UPDATE)
    @Transactional
    public RoleResult restoreRole(String role, String ifMatch, long restrictionId, Reason reason) {
        RolePermissions before = lockedRole(role, ifMatch, currentUser.getObject());
        RestrictionRow row = active(changes.activeRestrictions(role, null), restrictionId);
        changes.closeRestriction(row.id());
        return roleChanged(role, before, PermissionAuditActions.PERMISSION_RESTORED, reason, details(row));
    }

    @BusinessCommand
    @RequiresPermission(function = Function.ACCOUNT_PERMISSION_PARAMETER, operation = Operation.UPDATE)
    @Transactional
    public RoleAssignmentService.Result restrictAccount(
            long accountId, String ifMatch, RestrictionInput input, Reason reason) {
        CurrentUser user = currentUser.getObject();
        Account account = lockedAccount(accountId, ifMatch, user);
        List<EffectivePermission> before = permissions.current(accountId);
        PermissionKey key = key(input);
        requireWithinMatrix(roleCodes(accountId), key, input.kind());
        requireNotRestricted(changes.activeRestrictions(null, accountId), key, input);
        long id = changes.insertRestriction(
                null,
                accountId,
                key,
                input.kind(),
                input.metricGroup(),
                input.channel(),
                user.accountId(),
                reason.value());
        requireManagerLeft(key);
        return accountChanged(
                account, before, PermissionAuditActions.PERMISSION_RESTRICTED, reason, details(id, input));
    }

    @BusinessCommand
    @RequiresPermission(function = Function.ACCOUNT_PERMISSION_PARAMETER, operation = Operation.UPDATE)
    @Transactional
    public RoleAssignmentService.Result restoreAccount(
            long accountId, String ifMatch, long restrictionId, Reason reason) {
        Account account = lockedAccount(accountId, ifMatch, currentUser.getObject());
        List<EffectivePermission> before = permissions.current(accountId);
        RestrictionRow row = active(changes.activeRestrictions(null, accountId), restrictionId);
        changes.closeRestriction(row.id());
        return accountChanged(account, before, PermissionAuditActions.PERMISSION_RESTORED, reason, details(row));
    }

    @BusinessCommand
    @RequiresPermission(function = Function.ACCOUNT_PERMISSION_PARAMETER, operation = Operation.UPDATE)
    @Transactional
    public RoleAssignmentService.Result grantCarePlanApproval(long accountId, String ifMatch, Reason reason) {
        CurrentUser user = currentUser.getObject();
        Account account = lockedAccount(accountId, ifMatch, user);
        List<EffectivePermission> before = permissions.current(accountId);
        if (!accounts.roles(accountId).contains(Role.NURSING_STAFF)) {
            throw new ConditionNotMetException(List.of(new UnmetCondition(
                    "FR-024", "Chỉ gán quyền Duyệt kế hoạch chăm sóc cho tài khoản có vai trò Điều dưỡng")));
        }
        if (changes.hasActiveGrant(accountId)) {
            throw new ConditionNotMetException(
                    List.of(new UnmetCondition("FR-024", "Tài khoản đã có quyền Duyệt kế hoạch chăm sóc gán thêm")));
        }
        changes.insertCarePlanApprovalGrant(accountId, user.accountId(), reason.value());
        return accountChanged(account, before, PermissionAuditActions.ADDITIONAL_PERMISSION_GRANTED, reason, GRANT);
    }

    @BusinessCommand
    @RequiresPermission(function = Function.ACCOUNT_PERMISSION_PARAMETER, operation = Operation.UPDATE)
    @Transactional
    public RoleAssignmentService.Result revokeCarePlanApproval(long accountId, String ifMatch, Reason reason) {
        Account account = lockedAccount(accountId, ifMatch, currentUser.getObject());
        List<EffectivePermission> before = permissions.current(accountId);
        if (!changes.hasActiveGrant(accountId)) {
            throw new ConditionNotMetException(
                    List.of(new UnmetCondition("FR-024", "Tài khoản không có quyền Duyệt kế hoạch chăm sóc gán thêm")));
        }
        changes.closeGrant(accountId);
        return accountChanged(account, before, PermissionAuditActions.ADDITIONAL_PERMISSION_REVOKED, reason, GRANT);
    }

    private static final Map<String, Object> GRANT = Map.of("function", "CARE_PLAN", "operation", "APPROVE");

    /** So và tăng {@code role.permission_version} trong một câu lệnh; trả quyền của vai trò trước thay đổi. */
    private RolePermissions lockedRole(String role, String ifMatch, CurrentUser user) {
        AccountService.requireManager(user);
        int version = changes.roleVersion(role).orElseThrow(NotFoundException::new);
        RolePermissions before = roleView(role);
        VersionEtag.requireMatch(ifMatch, version, before);
        if (!changes.bumpRoleVersion(role, version)) {
            throw new VersionMismatchException(roleView(role));
        }
        return before;
    }

    private Account lockedAccount(long accountId, String ifMatch, CurrentUser user) {
        AccountService.requireManager(user);
        Account account = accounts.lock(accountId).orElseThrow(NotFoundException::new);
        VersionEtag.requireMatch(ifMatch, account.permissionVersion(), accountView(account));
        return account;
    }

    private RoleResult roleChanged(
            String role, RolePermissions before, AuditAction action, Reason reason, Map<String, Object> details) {
        RolePermissions after = roleView(role);
        auditLog.write(AuditLogEntry.builder()
                .action(action)
                .entity(PermissionAuditActions.ROLE_ENTITY, role)
                .before(Map.of("permissions", summary(before.current())))
                .after(Map.of("permissions", summary(after.current())))
                .reason(reason)
                .details(details)
                .build());
        return new RoleResult(after, changes.roleVersion(role).orElseThrow());
    }

    private RoleAssignmentService.Result accountChanged(
            Account account,
            List<EffectivePermission> before,
            AuditAction action,
            Reason reason,
            Map<String, Object> details) {
        accounts.bumpPermissionVersion(account.id());
        Account updated = accounts.find(account.id()).orElseThrow();
        AccountPermissions after = accountView(updated);
        auditLog.write(AuditLogEntry.builder()
                .action(action)
                .entity(AccountAuditActions.ENTITY, account.id())
                .before(Map.of("permissions", summary(before)))
                .after(Map.of("permissions", summary(after.current())))
                .reason(reason)
                .details(details)
                .build());
        return new RoleAssignmentService.Result(after, updated.permissionVersion());
    }

    /** FR-025: khóa có trong ô ma trận của một trong các vai trò; hạ phạm vi chỉ khi có ô toàn viện. */
    private void requireWithinMatrix(Set<String> roles, PermissionKey key, RestrictionKind kind) {
        Set<PermissionScope> scopes = changes.matrixScopes(roles, key);
        if (scopes.isEmpty()) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "EXCEEDS_MAXIMUM_PERMISSION",
                    "Quyền này không có trong ma trận quyền tối đa của vai trò (FR-025)");
        }
        if (kind == RestrictionKind.LOWER_SCOPE && !scopes.contains(PermissionScope.FACILITY)) {
            throw new ConditionNotMetException(
                    List.of(new UnmetCondition("FR-025", "Chỉ hạ phạm vi được từ toàn viện xuống theo phân công")));
        }
    }

    private static void requireNotRestricted(List<RestrictionRow> active, PermissionKey key, RestrictionInput input) {
        boolean exists = active.stream()
                .anyMatch(r -> r.key().equals(key)
                        && r.kind() == input.kind()
                        && Objects.equals(r.metricGroup(), input.metricGroup())
                        && Objects.equals(r.channel(), input.channel()));
        if (exists) {
            throw new ConditionNotMetException(
                    List.of(new UnmetCondition("FR-025", "Quyền này đã được thu hẹp như vậy")));
        }
    }

    /**
     * FR-012: sau thu hẹp khóa {@code ACCOUNT_PERMISSION_PARAMETER}, phải còn tài khoản Quản lý viện Hoạt động giữ thao
     * tác đó toàn viện, không điều kiện (dưới khóa các dòng gán vai trò AC-01).
     */
    private void requireManagerLeft(PermissionKey key) {
        if (key.function() != Function.ACCOUNT_PERMISSION_PARAMETER) {
            return;
        }
        boolean left = changes.activeManagers().stream()
                .anyMatch(id -> calculator.at(id, time.now()).getOrDefault(key, Set.of()).stream()
                        .anyMatch(g -> g.scope() == PermissionScope.FACILITY
                                && g.footnotes().isEmpty()));
        if (!left) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "NO_MANAGER_LEFT",
                    "Viện phải còn ít nhất một Quản lý viện có quyền cấu hình tài khoản và phân quyền (FR-012)");
        }
    }

    private static RestrictionRow active(List<RestrictionRow> rows, long restrictionId) {
        return rows.stream().filter(r -> r.id() == restrictionId).findFirst().orElseThrow(NotFoundException::new);
    }

    private static PermissionKey key(RestrictionInput input) {
        Function function;
        try {
            function = Function.valueOf(input.function());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new InvalidValueException("Mã chức năng không hợp lệ");
        }
        boolean detailed = input.metricGroup() != null || input.channel() != null;
        if (detailed && function != Function.DASHBOARD_REPORT) {
            throw new InvalidValueException(
                    "Nhóm chỉ tiêu, kênh chỉ dùng với chức năng Dashboard, báo cáo (feature 016 FR-010a)");
        }
        return new PermissionKey(function, input.operation());
    }

    private RolePermissions roleView(String role) {
        RolePermissionCalculator.RolePermissions computed = calculator.ofRole(role, time.now());
        return new RolePermissions(
                role,
                AccountPermissionsQuery.all(computed.maximum()),
                permissions.roleRestrictions(role),
                AccountPermissionsQuery.all(computed.current()));
    }

    private AccountPermissions accountView(Account account) {
        return permissions.of(account.id(), account.status().name(), List.copyOf(roleCodesSorted(account.id())));
    }

    private Set<String> roleCodes(long accountId) {
        return accounts.roles(accountId).stream().map(Role::code).collect(Collectors.toSet());
    }

    private List<String> roleCodesSorted(long accountId) {
        return roleCodes(accountId).stream().sorted().toList();
    }

    private static List<String> summary(List<EffectivePermission> permissions) {
        return permissions.stream()
                .map(p -> p.function() + "/" + p.operation() + "/" + p.scope())
                .distinct()
                .toList();
    }

    private static Map<String, Object> details(long restrictionId, RestrictionInput input) {
        Map<String, Object> details = new HashMap<>();
        details.put("restrictionId", restrictionId);
        details.put("function", input.function());
        details.put("operation", input.operation().name());
        details.put("kind", input.kind().name());
        if (input.metricGroup() != null) {
            details.put("metricGroup", input.metricGroup());
        }
        if (input.channel() != null) {
            details.put("channel", input.channel());
        }
        return details;
    }

    private static Map<String, Object> details(RestrictionRow row) {
        return details(
                row.id(),
                new RestrictionInput(
                        row.key().function().name(),
                        row.key().operation(),
                        row.kind(),
                        row.metricGroup(),
                        row.channel()));
    }
}
