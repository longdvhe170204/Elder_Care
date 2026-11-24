package com.eldercare.backend.care.internal.task.service;

import com.eldercare.backend.administration.account.api.*;
import com.eldercare.backend.administration.account.api.dto.*;
import com.eldercare.backend.care.internal.task.domain.CareTask;
import com.eldercare.backend.care.internal.task.domain.CareTaskHistoryFilter;
import com.eldercare.backend.care.internal.task.persistence.CareTaskStore;
import com.eldercare.backend.common.error.PermissionDeniedException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lịch sử công việc đã lập của màn Lập công việc ca, không theo ca hay ngày (FR-017): việc lập gần nhất trước, phân trang, lọc
 * theo tầng, người cao tuổi, loại công việc, người làm; chỉ trong phạm vi quyền xem lập công việc của người gọi.
 */
@Service
@Transactional(readOnly = true)
public class CareTaskHistory {
    static final PermissionKey VIEW = new PermissionKey(Function.SHIFT_TASK_PLANNING, Operation.VIEW);
    static final int MAX_SIZE = 100;

    public record Page(List<CareTask> items, long total) {}

    private final CareTaskStore tasks;
    private final ScopeApi scopes;
    private final PermissionApi permissions;
    private final CurrentUser user;

    public CareTaskHistory(CareTaskStore tasks, ScopeApi scopes, PermissionApi permissions, CurrentUser user) {
        this.tasks = tasks;
        this.scopes = scopes;
        this.permissions = permissions;
        this.user = user;
    }

    public Page list(Long floor, Long resident, Long taskType, Long performer, int page, int size) {
        if (!permissions.permissions(user).contains(VIEW)
                || (floor != null
                        && !permissions
                                .hasPermission(user, VIEW, new Target("M04.FLOOR", Long.toString(floor), null, floor))
                                .allowed()))
            throw new PermissionDeniedException("Không có quyền xem công việc đã lập trong phạm vi này");
        int limit = Math.clamp(size, 1, MAX_SIZE);
        int offset = Math.max(page, 0) * limit;
        var filter = new CareTaskHistoryFilter(floor, resident, taskType, performer);
        var scope = scopes.filter(user, VIEW);
        return new Page(tasks.history(filter, scope, offset, limit), tasks.historyCount(filter, scope));
    }
}
