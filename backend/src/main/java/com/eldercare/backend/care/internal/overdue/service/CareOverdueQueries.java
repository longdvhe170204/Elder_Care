package com.eldercare.backend.care.internal.overdue.service;

import com.eldercare.backend.administration.account.api.*;
import com.eldercare.backend.administration.account.api.dto.*;
import com.eldercare.backend.care.internal.task.domain.CareTask;
import com.eldercare.backend.care.internal.task.persistence.CareTaskStore;
import com.eldercare.backend.care.internal.task.service.CareTaskService;
import com.eldercare.backend.common.error.PermissionDeniedException;
import com.eldercare.backend.resident.api.ResidentApi;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CareOverdueQueries {
    private static final PermissionKey VIEW = new PermissionKey(Function.OVERDUE_TASK_HANDLING, Operation.VIEW);
    private final CareTaskStore tasks;
    private final ScopeApi scopes;
    private final PermissionApi permissions;
    private final CurrentUser user;
    private final ResidentApi residents;

    public CareOverdueQueries(
            CareTaskStore tasks, ScopeApi scopes, PermissionApi permissions, CurrentUser user, ResidentApi residents) {
        this.tasks = tasks;
        this.scopes = scopes;
        this.permissions = permissions;
        this.user = user;
        this.residents = residents;
    }

    public List<CareTask> list(Long floor) {
        if (!permissions.permissions(user).contains(VIEW)
                || (floor != null
                        && !permissions
                                .hasPermission(user, VIEW, new Target("M04.FLOOR", Long.toString(floor), null, floor))
                                .allowed()))
            throw new PermissionDeniedException("Không có quyền xem công việc quá hạn trong phạm vi này");
        return tasks.overdue(floor, scopes.filter(user, VIEW)).stream()
                .filter(t -> t.residentId() == null || !residents.isFinalStatus(t.residentId()))
                .filter(t -> permissions
                        .hasPermission(user, VIEW, CareTaskService.target(t))
                        .allowed())
                .toList();
    }
}
