package com.eldercare.backend.care.internal.result.service;

import com.eldercare.backend.administration.account.api.RequiresPermission;
import com.eldercare.backend.administration.account.api.dto.*;
import com.eldercare.backend.care.internal.result.domain.CareResult;
import com.eldercare.backend.care.internal.result.persistence.CareResultStore;
import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CareResultQueries {
    private final CareResultStore results;

    public CareResultQueries(CareResultStore results) {
        this.results = results;
    }

    @RequiresPermission(function = Function.TASK_CHECKLIST, operation = Operation.VIEW)
    public Optional<CareResult> original(Target target, long taskId) {
        return results.find(taskId);
    }

    @RequiresPermission(function = Function.TASK_CHECKLIST, operation = Operation.VIEW)
    public List<CareResult> history(Target target, long resident, Instant from, Instant to, Set<String> types) {
        return results.history(resident, from, to).stream()
                .filter(r -> types.isEmpty() || types.contains(r.resultType()))
                .toList();
    }
}
