package com.eldercare.backend.billing.internal.governance.service;

import com.eldercare.backend.administration.account.api.*;
import com.eldercare.backend.administration.account.api.dto.*;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.error.PermissionDeniedException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

/** FR-017/043: kiểm đủ quyền 002 và vai trò chịu trách nhiệm cho lệnh tài chính. */
@Service
public class CostAccess {
    private final ObjectProvider<CurrentUser> users;
    private final PermissionApi permissions;

    public CostAccess(ObjectProvider<CurrentUser> users, PermissionApi permissions) {
        this.users = users;
        this.permissions = permissions;
    }

    public CurrentUser user() {
        return users.getObject();
    }

    public long require(Role role, Function function, Operation operation, String type, long id, Long resident) {
        var user = user();
        if (!user.roles().contains(role)
                || (role == Role.OFFICE_STAFF && user.roles().contains(Role.NURSING_HOME_MANAGER)))
            throw new PermissionDeniedException("Vai trò không được thực hiện lệnh này");
        viewOrCommand(function, operation, type, id, resident);
        return user.accountId();
    }

    public void viewOrCommand(Function function, Operation operation, String type, long id, Long resident) {
        if (!permissions
                .hasPermission(
                        user(),
                        new PermissionKey(function, operation),
                        id == 0 ? Target.UNSPECIFIED : new Target(type, Long.toString(id), resident, null))
                .allowed()) throw new PermissionDeniedException("Không có quyền với chi phí này");
    }
}
