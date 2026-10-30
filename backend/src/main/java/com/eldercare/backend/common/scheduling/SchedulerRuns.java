package com.eldercare.backend.common.scheduling;

import com.eldercare.backend.common.time.BusinessTime;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

/**
 * Ghi lần chạy của job ra {@code logs/jobs.log} (log có cấu trúc, logger {@code jobs}, research R-15 mục 4): mã lần chạy là
 * UUID, đặt vào MDC {@code jobRunId} trong lúc chạy và dùng làm {@code schedulerRunId} trong căn cứ nhật ký (FR-045).
 * Thời điểm lấy từ đồng hồ của ứng dụng.
 */
@Component
public class SchedulerRuns {

    public static final String MDC_KEY = "jobRunId";
    private static final Logger JOBS = LoggerFactory.getLogger("jobs");

    public enum Result {
        SUCCEEDED,
        FAILED
    }

    private final BusinessTime time;

    public SchedulerRuns(BusinessTime time) {
        this.time = time;
    }

    /** Bắt đầu một lần chạy, trả mã lần chạy. */
    public String start(String job) {
        String runId = UUID.randomUUID().toString();
        MDC.put(MDC_KEY, runId);
        JOBS.atInfo()
                .addKeyValue("job", job)
                .addKeyValue("event", "START")
                .addKeyValue("businessTime", time.now())
                .log("Bắt đầu {}", job);
        return runId;
    }

    /** Kết thúc lần chạy với kết quả và số bản ghi đã xử lý. */
    public void finish(String runId, Result result, int processedCount) {
        try (MDC.MDCCloseable ignored = MDC.putCloseable(MDC_KEY, runId)) {
            JOBS.atInfo()
                    .addKeyValue("event", "FINISH")
                    .addKeyValue("result", result.name())
                    .addKeyValue("processedCount", processedCount)
                    .addKeyValue("businessTime", time.now())
                    .log("Kết thúc lần chạy: {}", result);
        }
    }
}
