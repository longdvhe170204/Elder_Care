package com.cambae.backend.m15.approval.internal;

import com.cambae.backend.common.Role;
import com.cambae.backend.common.error.InvalidValueException;
import com.cambae.backend.m15.account.api.AccountApi;
import com.cambae.backend.m15.account.api.ApproverApi;
import com.cambae.backend.m15.account.api.CurrentUser;
import com.cambae.backend.m15.account.api.PermissionApi;
import com.cambae.backend.m15.account.api.ScopeApi;
import com.cambae.backend.m15.approval.api.ApprovalRequestView;
import com.cambae.backend.m15.approval.api.spi.RequestTypeHandler;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * Ai được quyết định và danh sách người duyệt của một yêu cầu, một hàm dùng chung cho lệnh Duyệt, Từ chối,
 * {@code allowedCommands}, danh sách người nhận khi gửi duyệt, nhắc và BR-M15-09 (contracts mục 1.5, "Danh sách người
 * duyệt"; bổ sung theo plan 004, Q-338). Quyền xét tại lúc gọi, không lưu sẵn (FR-034a).
 */
@Component
class ApproverDirectory {

    private final ObjectProvider<RequestTypeHandler> handlers;
    private final ObjectProvider<PermissionApi> permissions;
    private final ObjectProvider<ScopeApi> scope;
    private final ObjectProvider<ApproverApi> approvers;
    private final ObjectProvider<AccountApi> accounts;

    ApproverDirectory(
            ObjectProvider<RequestTypeHandler> handlers,
            ObjectProvider<PermissionApi> permissions,
            ObjectProvider<ScopeApi> scope,
            ObjectProvider<ApproverApi> approvers,
            ObjectProvider<AccountApi> accounts) {
        this.handlers = handlers;
        this.permissions = permissions;
        this.scope = scope;
        this.approvers = approvers;
        this.accounts = accounts;
    }

    RequestTypeHandler handler(String type) {
        Map<String, RequestTypeHandler> byType =
                handlers.orderedStream().collect(Collectors.toMap(RequestTypeHandler::typeCode, h -> h));
        RequestTypeHandler handler = byType.get(type);
        if (handler == null) {
            throw new InvalidValueException("Không có loại yêu cầu " + type);
        }
        return handler;
    }

    /** Có quyền duyệt loại này, trong phạm vi, và có vai trò trong giới hạn của loại nếu có (FR-034, Q-338). */
    boolean canDecide(CurrentUser user, ApprovalRequestView request, RequestTypeHandler handler) {
        return permissions
                        .getObject()
                        .hasPermission(user, handler.approvePermission(), request.target())
                        .allowed()
                && scope.getObject().inScope(user, request.target())
                && withinRestriction(user.accountId(), handler.approverRoleRestriction(request));
    }

    /** Xem được: người yêu cầu, hoặc người có quyền tạo hay duyệt loại này trong phạm vi. */
    boolean canView(CurrentUser user, ApprovalRequestView request, RequestTypeHandler handler) {
        if (request.requesterId() == user.accountId()) {
            return true;
        }
        PermissionApi permission = permissions.getObject();
        boolean typeAccess = permission
                        .hasPermission(user, handler.approvePermission(), request.target())
                        .allowed()
                || permission
                        .hasPermission(user, handler.createPermission(), request.target())
                        .allowed();
        return typeAccess && scope.getObject().inScope(user, request.target());
    }

    /** Danh sách tài khoản có quyền duyệt, đã lọc giới hạn vai trò (contracts mục 1.5). */
    List<Long> approvers(ApprovalRequestView request, RequestTypeHandler handler) {
        Optional<Set<Role>> restriction = handler.approverRoleRestriction(request);
        return approvers.getObject().approvers(handler.approvePermission(), request.target()).stream()
                .filter(id -> withinRestriction(id, restriction))
                .toList();
    }

    boolean allManagers(List<Long> accountIds) {
        return accountIds.stream().allMatch(id -> accounts.getObject().hasRole(id, Role.NURSING_HOME_MANAGER));
    }

    private boolean withinRestriction(long accountId, Optional<Set<Role>> restriction) {
        return restriction
                .map(roles -> accounts.getObject().hasRole(accountId, roles.toArray(Role[]::new)))
                .orElse(true);
    }
}
