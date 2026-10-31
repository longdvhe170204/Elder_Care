package com.eldercare.backend.administration.approval.internal.service;

import com.eldercare.backend.administration.approval.internal.persistence.ApprovalRequestStore;
import com.eldercare.backend.common.scheduling.SchedulerRuns;
import com.eldercare.backend.common.time.BusinessTime;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Một đợt áp dụng các yêu cầu Đã duyệt đến hạn (000 T093; BR-M15-10, FR-037, FR-037a, FR-037b): theo thứ tự
 * {@code (ngày hiệu lực thực tế, thời điểm quyết định)}, mỗi yêu cầu một transaction qua {@link RequestApplier}; lỗi ở
 * một yêu cầu không chặn các yêu cầu sau.
 */
@Service
public class DueRequestApplication {

    static final String JOB = "APPLY_DUE_REQUESTS";

    private final ApprovalRequestStore store;
    private final RequestApplier applier;
    private final SchedulerRuns runs;
    private final BusinessTime time;

    DueRequestApplication(ApprovalRequestStore store, RequestApplier applier, SchedulerRuns runs, BusinessTime time) {
        this.store = store;
        this.applier = applier;
        this.runs = runs;
        this.time = time;
    }

    /** Chạy một đợt, trả mã lần chạy (UUID). */
    public String run() {
        String runId = runs.start(JOB);
        List<Long> due = store.dueApproved(time.today());
        int processed = 0;
        for (long id : due) {
            if (applier.applySafely(id, runId)) {
                processed++;
            }
        }
        runs.finish(runId, SchedulerRuns.Result.SUCCEEDED, processed);
        return runId;
    }
}
