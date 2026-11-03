package com.eldercare.backend.care.internal.rules.service;

import com.eldercare.backend.administration.correction.api.CorrectionApi;
import com.eldercare.backend.care.internal.result.domain.CareResult;
import com.eldercare.backend.care.internal.result.persistence.CareResultStore;
import com.eldercare.backend.care.internal.task.domain.CareTask;
import com.eldercare.backend.care.internal.task.persistence.CareTaskStore;
import com.eldercare.backend.common.error.BusinessException;
import java.time.Instant;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/** Projection giá trị hiện hành; VOID được bỏ trước khi tính quy tắc. */
@Service
public class CurrentCareResults {
    private final CareResultStore results;
    private final CareTaskStore tasks;
    private final CorrectionApi corrections;

    public CurrentCareResults(CareResultStore results, CareTaskStore tasks, CorrectionApi corrections) {
        this.results = results;
        this.tasks = tasks;
        this.corrections = corrections;
    }

    public record Value(CareTask task, CareResult original, Map<?, ?> values) {}

    public List<Value> read(long resident, Instant from, Instant to) {
        var rows = results.history(resident, from, to);
        var changed = corrections.current(
                "M04.CARE_RESULT", rows.stream().map(CareResult::id).toList());
        var current = new ArrayList<Value>();
        for (var row : rows) {
            var correction = changed.get(row.id());
            if (correction != null && correction.voided()) continue;
            Map<?, ?> values = row.values();
            if (correction != null && correction.corrected()) {
                if (!(correction.content() instanceof Map<?, ?> map))
                    throw new BusinessException(
                            HttpStatus.SERVICE_UNAVAILABLE,
                            "CARE_SOURCE_UNAVAILABLE",
                            "Chưa đọc được nội dung đính chính kết quả");
                values = map;
            }
            current.add(new Value(tasks.get(row.id(), false), row, values));
        }
        current.sort(Comparator.comparing((Value v) -> v.task().plannedAt())
                .thenComparingLong(v -> v.task().id())
                .reversed());
        return List.copyOf(current);
    }
}
