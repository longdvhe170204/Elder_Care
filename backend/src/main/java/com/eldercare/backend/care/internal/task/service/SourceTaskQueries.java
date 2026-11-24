package com.eldercare.backend.care.internal.task.service;

import com.eldercare.backend.administration.account.api.AccountApi;
import com.eldercare.backend.care.api.SourceTaskReadApi;
import com.eldercare.backend.care.api.dto.SourceTaskView;
import com.eldercare.backend.care.api.dto.TaskStatus;
import com.eldercare.backend.care.api.spi.TaskSourcePort.SourceType;
import com.eldercare.backend.care.internal.result.domain.CareResult;
import com.eldercare.backend.care.internal.result.persistence.CareResultStore;
import com.eldercare.backend.care.internal.task.domain.CareTask;
import com.eldercare.backend.care.internal.task.persistence.CareTaskStore;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Bản thật {@link SourceTaskReadApi}: chỉ đọc, không kiểm quyền (module giữ căn cứ đã kiểm). */
@Service
@Transactional(readOnly = true)
public class SourceTaskQueries implements SourceTaskReadApi {
    private final CareTaskStore tasks;
    private final CareResultStore results;
    private final AccountApi accounts;

    public SourceTaskQueries(CareTaskStore tasks, CareResultStore results, AccountApi accounts) {
        this.tasks = tasks;
        this.results = results;
        this.accounts = accounts;
    }

    @Override
    public List<SourceTaskView> bySource(Set<SourceType> types, long sourceId) {
        return types.stream()
                .flatMap(type -> tasks.forSource(type, sourceId).stream())
                .filter(task -> task.status() != TaskStatus.CANCELLED)
                .sorted(Comparator.comparing(CareTask::plannedAt).thenComparing(CareTask::id))
                .map(this::view)
                .toList();
    }

    @Override
    public Set<Long> withOpenTask(Set<SourceType> types, Collection<Long> sourceIds) {
        return types.isEmpty() || sourceIds.isEmpty() ? Set.of() : tasks.sourcesWithOpenTask(types, sourceIds);
    }

    private SourceTaskView view(CareTask task) {
        var closure = tasks.closure(task.id());
        String performer = task.performerId() == null
                ? null
                : accounts.byStaff(task.performerId())
                        .flatMap(account -> accounts.fullName(account.accountId()))
                        .orElse(null);
        return new SourceTaskView(
                task.id(),
                task.basisType(),
                task.basisId(),
                task.shiftId(),
                task.data().taskTypeName(),
                task.data().content(),
                task.plannedAt(),
                task.windowTo(),
                task.importance(),
                performer,
                task.performerId() == null,
                task.status(),
                closure.at(),
                results.find(task.id()).map(SourceTaskQueries::note).orElse(null),
                closure.reason());
    }

    private static String note(CareResult result) {
        Object note = result.values().get("note");
        return note == null ? null : note.toString();
    }
}
