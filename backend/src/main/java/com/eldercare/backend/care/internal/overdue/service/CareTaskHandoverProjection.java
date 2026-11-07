package com.eldercare.backend.care.internal.overdue.service;

import com.eldercare.backend.administration.account.api.spi.BedAllocationApi;
import com.eldercare.backend.care.api.TaskHandoverApi;
import com.eldercare.backend.care.api.dto.TaskHandoverView;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

/** Chữ ký tương thích008 được giữ nguyên, projection đầy đủ do HandoverTaskApi cung cấp. */
@Service
@org.springframework.context.annotation.Fallback
public class CareTaskHandoverProjection implements TaskHandoverApi {
    private final CareHandoverQueries queries;
    private final com.eldercare.backend.care.internal.task.persistence.CareTaskStore tasks;
    private final BedAllocationApi beds;

    public CareTaskHandoverProjection(
            CareHandoverQueries queries,
            com.eldercare.backend.care.internal.task.persistence.CareTaskStore tasks,
            BedAllocationApi beds) {
        this.queries = queries;
        this.tasks = tasks;
        this.beds = beds;
    }

    private TaskHandoverView row(long id) {
        var t = tasks.get(id, false);
        Long room = t.residentId() == null
                ? ("ROOM".equals(t.locationType()) ? t.locationId() : null)
                : beds.openAt(t.residentId(), t.plannedAt())
                        .map(b -> b.roomId())
                        .orElse(null);
        return new TaskHandoverView(
                t.id(),
                t.residentId(),
                room,
                t.data().taskTypeName(),
                t.plannedAt(),
                t.status().name(),
                t.importance().name(),
                t.locationType(),
                t.locationId(),
                t.floorAreaId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskHandoverView> unclosed(long floor, long shiftId) {
        return queries.rows(floor, shiftId).stream().map(t -> row(t.id())).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, TaskHandoverView> currentStatus(Collection<Long> ids) {
        var result = new LinkedHashMap<Long, TaskHandoverView>();
        for (long id : new TreeSet<>(ids)) result.put(id, row(id));
        return Map.copyOf(result);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void recordedInHandover(long handoverId, Collection<Long> ids) {
        queries.recordedInHandover(handoverId, ids);
    }
}
